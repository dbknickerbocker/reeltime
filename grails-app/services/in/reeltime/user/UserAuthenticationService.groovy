package in.reeltime.user

class UserAuthenticationService {

    def isUserLoggedIn() {

    }

    def getLoggedInUser() {
        User.findByUsername('bob')
    }
}
