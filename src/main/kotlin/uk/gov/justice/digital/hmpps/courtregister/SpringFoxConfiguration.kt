package uk.gov.justice.digital.hmpps.courtregister

import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import uk.gov.justice.digital.hmpps.courtregister.resource.CourtResource
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

@Configuration
@EnableSwagger2
@Import(BeanValidatorPluginsConfiguration::class)
class SpringFoxConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version

  @Bean
  fun api(): Docket {
    val apiInfo = ApiInfo("Court Register API Documentation", "API for providing Court information.",
        version, "", Contact("HMPPS Digital Studio", "", "feedback@digital.justice.gov.uk"),
        "", "", emptyList())
    val docket = Docket(DocumentationType.SWAGGER_2)
        .useDefaultResponseMessages(false)
        .apiInfo(apiInfo)
        .select()
        .apis(RequestHandlerSelectors.basePackage(CourtResource::class.java.getPackage().getName()))
        .paths(PathSelectors.any())
        .build()
    docket.genericModelSubstitutes(Optional::class.java)
    docket.directModelSubstitute(ZonedDateTime::class.java, Date::class.java)
    docket.directModelSubstitute(LocalDateTime::class.java, Date::class.java)
    return docket
  }
}
