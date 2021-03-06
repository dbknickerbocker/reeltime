package in.reeltime.user

import grails.plugin.springsecurity.annotation.Secured
import in.reeltime.common.AbstractController
import in.reeltime.search.PagedListCommand

import static in.reeltime.common.ContentTypes.APPLICATION_JSON
import static javax.servlet.http.HttpServletResponse.SC_CREATED
import static javax.servlet.http.HttpServletResponse.SC_OK

class UserFollowingController extends AbstractController {

    def userService
    def userFollowingService
    def authenticationService

    @Secured(["#oauth2.isUser() and #oauth2.hasScope('users-write')"])
    def followUser(UsernameCommand command) {
        handleCommandRequest(command) {
            def currentUser = authenticationService.currentUser
            def userToFollow = userService.loadUser(command.username)

            userFollowingService.startFollowingUser(currentUser, userToFollow)
            render(status: SC_CREATED)
        }
    }

    @Secured(["#oauth2.isUser() and #oauth2.hasScope('users-write')"])
    def unfollowUser(UsernameCommand command) {
        handleCommandRequest(command) {
            def currentUser = authenticationService.currentUser
            def userToUnfollow = userService.loadUser(command.username)

            userFollowingService.stopFollowingUser(currentUser, userToUnfollow)
            render(status: SC_OK)
        }
    }

    @Secured(["#oauth2.hasScope('users-read')"])
    def listFollowers(UsernameCommand usernameCommand, PagedListCommand pagedListCommand) {
        log.debug "Listing followers for user [${usernameCommand.username}] on page [${pagedListCommand.page}]"

        handleMultipleCommandRequest([usernameCommand, pagedListCommand]) {
            def user = userService.loadUser(usernameCommand.username)
            render(status: SC_OK, contentType: APPLICATION_JSON) {
                marshall(users: userFollowingService.listFollowersForFollowee(user, pagedListCommand.page))
            }
        }
    }

    @Secured(["#oauth2.hasScope('users-read')"])
    def listFollowees(UsernameCommand usernameCommand, PagedListCommand pagedListCommand) {
        log.debug "Listing followees for user [${usernameCommand.username}] on page [${pagedListCommand.page}]"

        handleMultipleCommandRequest([usernameCommand, pagedListCommand]) {
            def user = userService.loadUser(usernameCommand.username)
            render(status: SC_OK, contentType: APPLICATION_JSON) {
                marshall(users: userFollowingService.listFolloweesForFollower(user, pagedListCommand.page))
            }
        }
    }
}
