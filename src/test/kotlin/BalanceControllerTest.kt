import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.ClickHouseContainer
import org.testcontainers.containers.Network
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BalanceControllerTest {

    companion object {
        private val network = Network.newNetwork()

        @Container
        val clickHouse1 = ClickHouseContainer("clickhouse/clickhouse-server:23.3")
            .withNetwork(network)
            .withNetworkAliases("clickhouse1")
            .withExposedPorts(8123)

        @Container
        val clickHouse2 = ClickHouseContainer("clickhouse/clickhouse-server:23.3")
            .withNetwork(network)
            .withNetworkAliases("clickhouse2")
            .withExposedPorts(8123)

        @BeforeAll
        @JvmStatic
        fun setup() {
            // Инициализация таблиц в обеих базах
            initClickHouseDatabase(clickHouse1)
            initClickHouseDatabase(clickHouse2)
        }

        private fun initClickHouseDatabase(container: ClickHouseContainer) {
            val connection = DriverManager.getConnection(container.jdbcUrl, container.username, container.password)
            connection.createStatement().use { stmt ->
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS test_data (
                        id UInt32,
                        name String,
                        value Float64
                    ) ENGINE = MergeTree()
                    ORDER BY id
                """.trimIndent())

                // Вставляем тестовые данные с разными значениями для идентификации сервера
                val serverId = if (container.networkAliases.contains("clickhouse1")) 1 else 2
                stmt.execute("""
                    INSERT INTO test_data VALUES
                    (1, 'Server $serverId - Item 1', ${serverId}.1),
                    (2, 'Server $serverId - Item 2', ${serverId}.2)
                """.trimIndent())
            }
            connection.close()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            network.close()
        }
    }

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `should balance requests between two clickhouse servers`() {
        // Настраиваем балансировщик на тестовые ClickHouse контейнеры
        System.setProperty("balance.targets",
            "http://${clickHouse1.host}:${clickHouse1.getMappedPort(8123)},http://${clickHouse2.host}:${clickHouse2.getMappedPort(8123)}")
        System.setProperty("balance.strategy", "ROUND_ROBIN")

        // Создаем WebClient для тестирования
        val client = WebTestClient.bindToServer()
            .baseUrl("https://localhost:$port")
            .build()

        // Делаем несколько запросов и проверяем, что ответы приходят от разных серверов
        val responses = mutableSetOf<String>()

        repeat(4) {
            client.get().uri("/balance/?query=SELECT name, value FROM test_data ORDER BY id")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .consumeWith { response ->
                    val body = String(response.responseBodyContent!!)
                    responses.add(body)
                    println("Response $it: $body")
                }
        }

        // Проверяем, что получили ответы от обоих серверов
        assert(responses.size > 1) {
            "Expected responses from both servers, but got only from one. Responses: $responses"
        }
    }

    @Test
    fun `should use sticky session correctly`() {
        // Настраиваем балансировщик
        System.setProperty("balance.targets",
            "http://${clickHouse1.host}:${clickHouse1.getMappedPort(8123)},http://${clickHouse2.host}:${clickHouse2.getMappedPort(8123)}")
        System.setProperty("balance.strategy", "STICKY_SESSION")

        val sessionId = UUID.randomUUID().toString()

        // Делаем несколько запросов с одной сессией
        val firstResponse = webTestClient.get()
            .uri("/balance/?query=SELECT name FROM test_data WHERE id = 1")
            .header("Cookie", "JSESSIONID=$sessionId")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult()
            .responseBody

        // Повторный запрос с той же сессией должен попасть на тот же сервер
        webTestClient.get()
            .uri("/balance/?query=SELECT name FROM test_data WHERE id = 1")
            .header("Cookie", "JSESSIONID=$sessionId")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo(firstResponse)
    }
}