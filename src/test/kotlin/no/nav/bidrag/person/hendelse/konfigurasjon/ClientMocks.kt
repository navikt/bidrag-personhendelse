package no.nav.bidrag.person.hendelse.konfigurasjon

import io.mockk.every
import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
class ClientMocks {
    @Bean
    @Primary
    fun mockFeatureToggleService(): FeatureToggleService {
        val mockFeatureToggleClient = mockk<FeatureToggleService>(relaxed = true)

        every {
            mockFeatureToggleClient.isEnabled(any())
        } returns true

        every {
            mockFeatureToggleClient.isEnabled(any(), any())
        } returns true

        return mockFeatureToggleClient
    }
}
