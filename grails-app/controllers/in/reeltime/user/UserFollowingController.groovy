package in.reeltime.user

import grails.plugin.springsecurity.annotation.Secured
import in.reeltime.common.AbstractController
import in.reeltime.common.CustomMarshaller
import in.reeltime.exceptions.UserNotFoundException

import static javax.servlet.http.HttpServletResponse.*
import static in.reeltime.common.ContentTypes.APPLICATION_JSON

class UserFollowingController extends AbstractController {

    def userService
    def userFollowingService
    def userAuthenticationService

    static allowedMethods = [
            followUser: 'POST', unfollowUser: 'DELETE',
            listFollowers: 'GET', listFollowees: 'GET'
    ]

    @Secured(["#oauth2.isUser() and #oauth2.hasScope('users-write')"])
    def followUser(String username) {
        handleSingleParamRequest(username, 'following.username.required') {
            def currentUser = userAuthenticationService.currentUser
            def userToFollow = userService.loadUser(username)

            userFollowingService.startFollowingUser(currentUser, userToFollow)
            render(status: SC_CREATED)
        }
    }

    @Secured(["#oauth2.isUser() and #oauth2.hasScope('users-write')"])
    def unfollowUser(String username) {
        handleSingleParamRequest(username, 'following.username.required') {
            def currentUser = userAuthenticationService.currentUser
            def userToUnfollow = userService.loadUser(username)

            userFollowingService.stopFollowingUser(currentUser, userToUnfollow)
            render(status: SC_OK)
        }
    }

    @Secured(["#oauth2.hasScope('users-read')"])
    def listFollowers(String username) {
        handleSingleParamRequest(username, 'following.username.required') {
            def user = userService.loadUser(username)
            render(status: SC_OK, contentType: APPLICATION_JSON) {
                marshall(userFollowingService.listFollowersForFollowee(user))
            }
        }
    }

    @Secured(["#oauth2.hasScope('users-read')"])
    def listFollowees(String username) {
        handleSingleParamRequest(username, 'following.username.required') {
            def user = userService.loadUser(username)
            render(status: SC_OK, contentType: APPLICATION_JSON) {
                marshall(userFollowingService.listFolloweesForFollower(user))
            }
        }
    }

    def handleUserNotFoundException(UserNotFoundException e) {
        exceptionErrorMessageResponse(e, 'following.unknown.username', SC_BAD_REQUEST)
    }

    def handleIllegalArgumentException(IllegalArgumentException e) {
        exceptionErrorMessageResponse(e, 'following.invalid.request', SC_BAD_REQUEST)
    }
}
