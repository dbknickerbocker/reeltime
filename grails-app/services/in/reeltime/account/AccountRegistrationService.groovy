package in.reeltime.account

import in.reeltime.exceptions.ConfirmationException
import in.reeltime.user.User
import java.security.MessageDigest

class AccountRegistrationService {

    def userService
    def clientService

    def accountConfirmationService

    def securityService
    def springSecurityService

    def localizedMessageService
    def mailService

    def fromAddress

    protected static final SALT_LENGTH = 8
    protected static final CONFIRMATION_CODE_LENGTH = 8
    protected static final ALLOWED_CHARACTERS = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'

    RegistrationResult registerUserAndClient(RegistrationCommand command, Locale locale) {

        def username = command.username
        def password = command.password
        def email = command.email
        def clientName = command.client_name

        def clientId = clientService.generateClientId()
        def clientSecret = clientService.generateClientSecret()

        def client = clientService.createClient(clientName, clientId, clientSecret)
        def user = userService.createUser(username, password, email, client)

        sendConfirmationEmail(user, locale)
        new RegistrationResult(clientId: clientId, clientSecret: clientSecret)
    }

    void sendConfirmationEmail(User user, Locale locale) {

        def code = securityService.generateSecret(CONFIRMATION_CODE_LENGTH, ALLOWED_CHARACTERS)
        def salt = securityService.generateSalt(SALT_LENGTH)

        def hashedCode = accountConfirmationService.hashConfirmationCode(code, salt)
        new AccountConfirmation(user: user, code: hashedCode, salt: salt).save()

        def localizedSubject = localizedMessageService.getMessage('registration.email.subject', locale)
        def localizedMessage = localizedMessageService.getMessage('registration.email.message', locale, [user.username, code])

        mailService.sendMail(user.email, fromAddress, localizedSubject, localizedMessage)
    }
}
