package app.dmessanger.admin

import app.dmessanger.auth.PasswordHasher
import app.dmessanger.auth.UserCredentialRepository
import app.dmessanger.chats.ChatRepository
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
    private val chats: ChatRepository,
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

        if (properties.seed.users.size >= 2) {
            chats.ensureDevDirectChat(
                firstLogin = properties.seed.users[0].login,
                secondLogin = properties.seed.users[1].login,
            )
            logger.info(
                "Ensured dev direct chat between {} and {}",
                properties.seed.users[0].login,
                properties.seed.users[1].login,
            )
        }
    }
}
