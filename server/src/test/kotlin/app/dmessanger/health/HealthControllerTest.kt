package app.dmessanger.health

import app.dmessanger.config.DmessangerProperties
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthControllerTest {
    @Test
    fun `health returns up status`() {
        val controller = HealthController(
            properties = DmessangerProperties(),
            buildProperties = null,
        )

        val response = controller.health()

        assertEquals("UP", response.status)
        assertEquals("dmessanger-backend", response.service)
    }
}
