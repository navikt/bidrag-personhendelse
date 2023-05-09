package no.nav.bidrag.person.hendelse.konfigurasjon

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import java.time.Duration

@EnableKafka
@Configuration
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
open class Kafkakonfig(val kafka: Kafka) {

    @Primary
    @ConfigurationProperties("spring.kafka")
    data class Kafka(
        val bootstrapServers: String
    )

    @Bean
    fun kafkaLeesahListenerContainerFactory(
        properties: KafkaProperties,
        kafkaOmstartFeilhåndterer: KafkaOmstartFeilhåndterer,
        environment: Environment
    ): ConcurrentKafkaListenerContainerFactory<Int, GenericRecord> {
        properties.properties[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = "true"
        val factory = ConcurrentKafkaListenerContainerFactory<Int, GenericRecord>()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.containerProperties.authExceptionRetryInterval = Duration.ofSeconds(2)
        factory.consumerFactory = DefaultKafkaConsumerFactory(
            properties.buildConsumerProperties().also {
                it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = OffsetResetStrategy.EARLIEST.toString().lowercase()
            }
        )
        factory.setCommonErrorHandler(kafkaOmstartFeilhåndterer)
        return factory
    }
/*
    @Bean
    fun producerFactory(): ProducerFactory<String, String> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafka.bootstrapServers
        configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> {
        return KafkaTemplate(producerFactory())
    }

    */
}
