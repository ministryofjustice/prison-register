package uk.gov.justice.digital.hmpps.prisonregister.dsl

import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.Agency
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyType
import uk.gov.justice.digital.hmpps.prisonregister.model.Court
import uk.gov.justice.digital.hmpps.prisonregister.model.Hospital
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuite
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOffice
import java.time.LocalDate

@DslMarker
annotation class DataDslMarker

@DataDslMarker
@Component
@Transactional
class Root(
  val courtBuilder: CourtBuilder,
  val hospitalBuilder: HospitalBuilder,
  val probationOfficeBuilder: ProbationOfficeBuilder,
  val policeCustodySuiteBuilder: PoliceCustodySuiteBuilder,
  val approvedPremisesBuilder: ApprovedPremisesBuilder,
  val agencyBuilder: AgencyBuilder,
  val prisonBuilder: PrisonBuilder,
) {
  fun agency(
    agencyId: String,
    name: String,
    description: String = name,
    active: Boolean = true,
    accessibleAccess: AccessibleAccess = AccessibleAccess.NONE,
    agencyType: AgencyType = AgencyType.PROBATION_CRC,
    inactiveDate: LocalDate? = null,
    cjitCode: String? = null,
    areaCode: String? = null,
    regionCode: String? = null,
    geographicalAreaCode: String? = null,
    payrollRegionCode: String? = null,
    localAuthorityCode: String? = null,
    dsl: AgencyBuilder.() -> Unit,
  ): Agency = agencyBuilder.build(
    agencyId = agencyId,
    name = name,
    description = description,
    active = active,
    accessibleAccess = accessibleAccess,
    agencyType = agencyType,
    inactiveDate = inactiveDate,
    cjitCode = cjitCode,
    areaCode = areaCode,
    regionCode = regionCode,
    geographicalAreaCode = geographicalAreaCode,
    payrollRegionCode = payrollRegionCode,
    localAuthorityCode = localAuthorityCode,
  ).also {
    dsl.invoke(agencyBuilder)
  }

  fun court(
    courtId: String,
    name: String,
    description: String = name,
    active: Boolean = true,
    inactiveDate: LocalDate? = null,
    courtTypeCode: String = "CC",
    cjitCode: String? = null,
    accessibleAccess: AccessibleAccess = AccessibleAccess.NONE,
    areaCode: String? = null,
    regionCode: String? = null,
    geographicalAreaCode: String? = null,
    payrollRegionCode: String? = null,
    localAuthorityCode: String? = null,
    dsl: CourtBuilder.() -> Unit,
  ): Court = courtBuilder.build(
    courtId = courtId,
    name = name,
    description = description,
    active = active,
    inactiveDate = inactiveDate,
    courtTypeCode = courtTypeCode,
    cjitCode = cjitCode,
    accessibleAccess = accessibleAccess,
    areaCode = areaCode,
    regionCode = regionCode,
    geographicalAreaCode = geographicalAreaCode,
    payrollRegionCode = payrollRegionCode,
    localAuthorityCode = localAuthorityCode,
  ).also {
    dsl.invoke(courtBuilder)
  }
  fun probationOffice(
    probationOfficeId: String,
    name: String,
    description: String = name,
    contact: String? = null,
    active: Boolean = true,
    accessibleAccess: AccessibleAccess = AccessibleAccess.NONE,
    inactiveDate: LocalDate? = null,
    cjitCode: String? = null,
    areaCode: String? = null,
    subareaCode: String? = null,
    regionCode: String? = null,
    geographicalAreaCode: String? = null,
    payrollRegionCode: String? = null,
    localAuthorityCode: String? = null,
    dsl: ProbationOfficeBuilder.() -> Unit,
  ): ProbationOffice = probationOfficeBuilder.build(
    probationOfficeId = probationOfficeId,
    name = name,
    description = description,
    contact = contact,
    active = active,
    accessibleAccess = accessibleAccess,
    inactiveDate = inactiveDate,
    cjitCode = cjitCode,
    areaCode = areaCode,
    subareaCode = subareaCode,
    regionCode = regionCode,
    geographicalAreaCode = geographicalAreaCode,
    payrollRegionCode = payrollRegionCode,
    localAuthorityCode = localAuthorityCode,
  ).also {
    dsl.invoke(probationOfficeBuilder)
  }

  fun approvedPremises(
    approvedPremisesId: String,
    name: String,
    description: String = name,
    contact: String? = null,
    active: Boolean = true,
    accessibleAccess: AccessibleAccess = AccessibleAccess.NONE,
    inactiveDate: LocalDate? = null,
    cjitCode: String? = null,
    areaCode: String? = null,
    regionCode: String? = null,
    geographicalAreaCode: String? = null,
    payrollRegionCode: String? = null,
    localAuthorityCode: String? = null,
    dsl: ApprovedPremisesBuilder.() -> Unit,
  ): uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremises = approvedPremisesBuilder.build(
    approvedPremisesId = approvedPremisesId,
    name = name,
    description = description,
    contact = contact,
    active = active,
    accessibleAccess = accessibleAccess,
    inactiveDate = inactiveDate,
    cjitCode = cjitCode,
    areaCode = areaCode,
    regionCode = regionCode,
    geographicalAreaCode = geographicalAreaCode,
    payrollRegionCode = payrollRegionCode,
    localAuthorityCode = localAuthorityCode,
  ).also {
    dsl.invoke(approvedPremisesBuilder)
  }

  fun policeCustodySuite(
    policeCustodySuiteId: String,
    name: String,
    description: String = name,
    active: Boolean = true,
    inactiveDate: LocalDate? = null,
    cjitCode: String? = null,
    areaCode: String? = null,
    regionCode: String? = null,
    geographicalAreaCode: String? = null,
    payrollRegionCode: String? = null,
    localAuthorityCode: String? = null,
    dsl: PoliceCustodySuiteBuilder.() -> Unit,
  ): PoliceCustodySuite = policeCustodySuiteBuilder.build(
    policeCustodySuiteId = policeCustodySuiteId,
    name = name,
    description = description,
    active = active,
    inactiveDate = inactiveDate,
    cjitCode = cjitCode,
    areaCode = areaCode,
    regionCode = regionCode,
    geographicalAreaCode = geographicalAreaCode,
    payrollRegionCode = payrollRegionCode,
    localAuthorityCode = localAuthorityCode,
  ).also {
    dsl.invoke(policeCustodySuiteBuilder)
  }

  fun hospital(
    hospitalId: String,
    name: String,
    description: String = name,
    active: Boolean = true,
    highSecurity: Boolean = true,
    inactiveDate: LocalDate? = null,
    cjitCode: String? = null,
    areaCode: String? = null,
    regionCode: String? = null,
    payrollRegionCode: String? = null,
    geographicalAreaCode: String? = null,
    localAuthorityCode: String? = null,
    dsl: HospitalBuilder.() -> Unit,
  ): Hospital = hospitalBuilder.build(
    hospitalId = hospitalId,
    name = name,
    description = description,
    active = active,
    highSecurity = highSecurity,
    inactiveDate = inactiveDate,
    cjitCode = cjitCode,
    areaCode = areaCode,
    geographicalAreaCode = geographicalAreaCode,
    regionCode = regionCode,
    payrollRegionCode = payrollRegionCode,
    localAuthorityCode = localAuthorityCode,
  ).also {
    dsl.invoke(hospitalBuilder)
  }

  fun prison(
    prisonId: String,
    name: String,
    description: String? = name,
    active: Boolean = true,
    male: Boolean = false,
    female: Boolean = false,
    contracted: Boolean = false,
    lthse: Boolean = false,
    prisonNameInWelsh: String? = null,
    inactiveDate: LocalDate? = null,
    dsl: PrisonBuilder.() -> Unit,
  ): Prison = prisonBuilder.build(
    prisonId = prisonId,
    name = name,
    description = description,
    active = active,
    male = male,
    female = female,
    contracted = contracted,
    lthse = lthse,
    prisonNameInWelsh = prisonNameInWelsh,
    inactiveDate = inactiveDate,
  ).also {
    dsl.invoke(prisonBuilder)
  }
}
