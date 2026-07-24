package app.dmessanger.admin

import app.dmessanger.auth.PasswordHasher
import app.dmessanger.auth.UserCredentialRepository
import app.dmessanger.config.DmessangerProperties
import app.dmessanger.users.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DevUserSeeder(
    private val properties: DmessangerProperties,
    private val users: UserRepository,
    private val credentials: UserCredentialRepository,
    private val passwordHasher: PasswordHasher,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!properties.seed.enabled) {
            logger.info("Dev user seed is disabled")
            return
        }

        properties.seed.users.forEach { seedUser ->
            val user = users.upsertSeedUser(
                login = seedUser.login,
                displayName = seedUser.displayName,
            )
            credentials.upsert(
                userId = user.id,
                passwordHash = passwordHasher.hash(seedUser.password),
            )
            logger.info("Seeded dev user login={}", seedUser.login)
        }
    }
}
