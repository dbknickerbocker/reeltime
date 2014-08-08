package in.reeltime.account

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import groovy.json.JsonSlurper
import in.reeltime.exceptions.RegistrationException
import in.reeltime.exceptions.ConfirmationException
import in.reeltime.message.LocalizedMessageService
import in.reeltime.user.User
import spock.lang.Specification
import in.reeltime.user.UserService
import spock.lang.Unroll

@TestFor(AccountController)
@Mock([User])
class AccountControllerSpec extends Specification {

    AccountRegistrationService accountRegistrationService
    AccountConfirmationService accountConfirmationService

    LocalizedMessageService localizedMessageService

    void setup() {
        accountRegistrationService = Mock(AccountRegistrationService)
        accountConfirmationService = Mock(AccountConfirmationService)
        localizedMessageService = Mock(LocalizedMessageService)

        controller.accountRegistrationService = accountRegistrationService
        controller.accountConfirmationService = accountConfirmationService
        controller.localizedMessageService = localizedMessageService

        defineBeans {
            userService(UserService)
        }
    }

    void "respond with client credentials upon successful registration"() {
        given:
        def username = 'foo'
        def password = 'secret'

        def email = 'foo@test.com'
        def clientName = 'something'

        and:
        def clientId = 'buzz'
        def clientSecret = 'bazz'

        and:
        def registrationResult = new RegistrationResult(clientId: clientId, clientSecret: clientSecret)

        and:
        params.username = username
        params.password = password
        params.email = email
        params.client_name = clientName

        and:
        def registrationCommandValidator = { RegistrationCommand command, Locale locale ->
            assert command.username == username
            assert command.password == password
            assert command.email == email
            assert command.client_name == clientName
            assert locale == request.locale
            return registrationResult
        }

        when:
        controller.register()

        then:
        response.status == 201
        response.contentType.startsWith('application/json')

        and:
        def json = new JsonSlurper().parseText(response.contentAsString) as Map
        json.size() == 2

        and:
        json.client_id == clientId
        json.client_secret == clientSecret

        and:
        1 * accountRegistrationService.registerUserAndClient(*_) >> { command, locale -> registrationCommandValidator(command, locale) }
    }

    void "registration exception is thrown"() {
        given:
        params.username = 'foo'
        params.password = 'secret'
        params.email = 'foo@test.com'
        params.client_name = 'something'

        and:
        def message = 'this is a test'

        when:
        controller.register()

        then:
        response.status == 503
        response.contentType.startsWith('application/json')

        and:
        def json = new JsonSlurper().parseText(response.contentAsString) as Map
        json.size() == 1

        and:
        json.errors == [message]

        and:
        1 * accountRegistrationService.registerUserAndClient(_, _) >> { throw new RegistrationException('TEST') }
        1 * localizedMessageService.getMessage('registration.internal.error', request.locale) >> message
    }

    @Unroll
    void "confirmation code must be present -- cannot be [#code]"() {
        given:
        def message = 'confirmation code required'

        and:
        params.code = code

        when:
        controller.confirm()

        then:
        response.status == 400
        response.contentType.startsWith('application/json')

        and:
        def json = new JsonSlurper().parseText(response.contentAsString) as Map
        json.size() == 1

        and:
        json.errors == [message]

        and:
        1 * localizedMessageService.getMessage('registration.confirmation.code.required', request.locale) >> message

        where:
        _   |   code
        _   |   null
        _   |   ''
    }

    void "pass confirmation code to service to complete account confirmation"() {
        given:
        params.code = 'let-me-in'

        when:
        controller.confirm()

        then:
        response.status == 200
        response.contentLength == 0

        and:
        1 * accountConfirmationService.confirmAccount('let-me-in')
    }

    void "handle confirmation error"() {
        given:
        def message = 'confirmation error'

        and:
        params.code = 'uh-oh'

        when:
        controller.confirm()

        then:
        response.status == 400
        response.contentType.startsWith('application/json')

        and:
        def json = new JsonSlurper().parseText(response.contentAsString) as Map
        json.size() == 1

        and:
        json.errors == [message]

        and:
        1 * accountConfirmationService.confirmAccount(_) >> { throw new ConfirmationException('TEST') }
        1 * localizedMessageService.getMessage('registration.confirmation.code.error', request.locale) >> message
    }
}