package in.reeltime.registration

import grails.validation.Validateable
import in.reeltime.user.User

@Validateable
class RegistrationCommand {

    def userService

    String username
    String password
    String client_name

    static constraints = {
        importFrom User, include: ['username', 'password']

        username validator: usernameMustBeAvailable
        password minSize: 6
        client_name blank: false, nullable: false
    }

    private static Closure usernameMustBeAvailable = { val, obj ->
        if(obj.userService.userExists(val)) {
            return 'unavailable'
        }
    }
}
