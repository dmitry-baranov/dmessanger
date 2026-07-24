package app.dmessanger.auth

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RefreshTokenServiceTest {
    private val service = RefreshTokenService()

    @Test
    fun `generated refresh tokens are random and hashable`() {
        val first = service.generate()
        val second = service.generate()

        assertNotEquals(first, second)
        assertNotEquals(service.hash(first), service.hash(second))
        assertTrue(first.length >= 32)
    }
}
