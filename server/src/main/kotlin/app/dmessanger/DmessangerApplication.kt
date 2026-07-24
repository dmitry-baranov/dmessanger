package app.dmessanger

import app.dmessanger.config.DmessangerProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(DmessangerProperties::class)
class DmessangerApplication

fun main(args: Array<String>) {
    runApplication<DmessangerApplication>(*args)
}
