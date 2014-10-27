package in.reeltime.reel

import grails.plugin.springsecurity.annotation.Secured
import in.reeltime.common.AbstractController
import in.reeltime.exceptions.AuthorizationException
import in.reeltime.exceptions.InvalidReelNameException
import in.reeltime.exceptions.ReelNotFoundException
import in.reeltime.exceptions.UserNotFoundException
import in.reeltime.exceptions.VideoNotFoundException
import in.reeltime.search.PagedListCommand

import static in.reeltime.common.ContentTypes.APPLICATION_JSON
import static javax.servlet.http.HttpServletResponse.*

class ReelController extends AbstractController {

    def reelService
    def reelVideoManagementService

    static allowedMethods = [
            listReels: 'GET',
            listUserReels: 'GET', addReel: 'POST', deleteReel: 'DELETE',
            listVideos: 'GET', addVideo: 'POST', removeVideo: 'DELETE'
    ]

    @Secured(["#oauth2.hasScope('reels-read')"])
    def listReels(PagedListCommand command) {
        log.debug "Listing all reels on page [${command.page}]"
        handleCommandRequest(command) {
            render(status: SC_OK, contentType: APPLICATION_JSON) {
                marshall(reelService.listReels(command.page))
            }
        }
    }

    @Secured(["#oauth2.hasScope('reels-read')"])
    def listUserReels(String username) {
        log.debug "Listing reels for user [$username]"
        handleSingleParamRequest(username, 'reel.username.required') {
            render(status: SC_OK, contentType: APPLICATION_JSON) {
                marshall(reelService.listReelsByUsername(username))
            }
        }
    }

    @Secured(["#oauth2.isUser() and #oauth2.hasScope('reels-write')"])
    def addReel(String name) {
        log.debug "Adding reel [$name]"
        handleSingleParamRequest(name, 'reel.name.required') {
            render(status: SC_CREATED, contentType: APPLICATION_JSON) {
                marshall(reelService.addReel(name))
            }
        }
    }

    @Secured(["#oauth2.isUser() and #oauth2.hasScope('reels-write')"])
    def deleteReel(ReelCommand command) {
        log.debug "Deleting reel [${command.reelId}]"
        handleCommandRequest(command) {
            reelService.deleteReel(command.reelId)
            render(status: SC_OK)
        }
    }

    @Secured(["#oauth2.hasScope('reels-read')"])
    def listVideos(ReelCommand command) {
        log.debug "Listing videos in reel [${command.reelId}]"
        handleCommandRequest(command) {
            render(status: SC_OK, contentType: APPLICATION_JSON) {
                marshall(reelVideoManagementService.listVideos(command.reelId))
            }
        }
    }

    @Secured(["#oauth2.isUser() and #oauth2.hasScope('reels-write')"])
    def addVideo(ReelVideoCommand command) {
        log.debug "Adding video [${command.videoId}] to reel [${command.reelId}]"
        handleCommandRequest(command) {
            reelVideoManagementService.addVideo(command.reelId, command.videoId)
            render(status: SC_CREATED)
        }
    }

    @Secured(["#oauth2.isUser() and #oauth2.hasScope('reels-write')"])
    def removeVideo(ReelVideoCommand command) {
        log.debug "Removing video [${command.videoId}] from reel [${command.reelId}]"
        handleCommandRequest(command) {
            reelVideoManagementService.removeVideo(command.reelId, command.videoId)
            render(status: SC_OK)
        }
    }

    def handleAuthorizationException(AuthorizationException e) {
        exceptionErrorMessageResponse(e, 'reel.unauthorized', SC_FORBIDDEN)
    }

    def handleUserNotFoundException(UserNotFoundException e) {
        exceptionErrorMessageResponse(e, 'reel.unknown.username', SC_NOT_FOUND)
    }

    def handleReelNotFoundException(ReelNotFoundException e) {
        exceptionErrorMessageResponse(e, 'reel.unknown', SC_NOT_FOUND)
    }

    def handleInvalidReelNameException(InvalidReelNameException e) {
        exceptionErrorMessageResponse(e, 'reel.invalid.name', SC_BAD_REQUEST)
    }

    def handleVideoNotFoundException(VideoNotFoundException e) {
        exceptionErrorMessageResponse(e, 'video.unknown', SC_NOT_FOUND)
    }
}
