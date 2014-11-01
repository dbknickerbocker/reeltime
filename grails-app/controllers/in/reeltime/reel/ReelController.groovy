package in.reeltime.reel

import grails.plugin.springsecurity.annotation.Secured
import in.reeltime.common.AbstractController
import in.reeltime.exceptions.AuthorizationException
import in.reeltime.exceptions.InvalidReelNameException
import in.reeltime.exceptions.ReelNotFoundException
import in.reeltime.exceptions.UserNotFoundException
import in.reeltime.exceptions.VideoNotFoundException
import in.reeltime.search.PagedListCommand
import in.reeltime.user.UsernameCommand
import in.reeltime.video.VideoCommand

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
    def listUserReels(UsernameCommand usernameCommand, PagedListCommand pagedListCommand) {
        log.debug "Listing reels for user [${usernameCommand.username}] on page [${pagedListCommand.page}]"
        handleMultipleCommandRequest([usernameCommand, pagedListCommand]) {
            render(status: SC_OK, contentType: APPLICATION_JSON) {
                marshall(reelService.listReelsByUsername(usernameCommand.username, pagedListCommand.page))
            }
        }
    }

    @Secured(["#oauth2.isUser() and #oauth2.hasScope('reels-write')"])
    def addReel(AddReelCommand command) {
        log.debug "Adding reel [${command.name}]"
        handleCommandRequest(command) {
            try {
                render(status: SC_CREATED, contentType: APPLICATION_JSON) {
                    marshall(reelService.addReel(command.name))
                }
            }
            catch(InvalidReelNameException e) {
                exceptionErrorMessageResponse(e, 'addReel.name.invalid', SC_BAD_REQUEST)
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
    def listVideos(ReelCommand reelCommand, PagedListCommand pagedListCommand) {
        log.debug "Listing videos in reel [${reelCommand.reelId}] on page [${pagedListCommand.page}]"
        handleMultipleCommandRequest([reelCommand, pagedListCommand]) {
            render(status: SC_OK, contentType: APPLICATION_JSON) {
                marshall(reelVideoManagementService.listVideosInReel(reelCommand.reelId, pagedListCommand.page))
            }
        }
    }

    @Secured(["#oauth2.isUser() and #oauth2.hasScope('reels-write')"])
    def addVideo(ReelCommand reelCommand, VideoCommand videoCommand) {
        log.debug "Adding video [${videoCommand.videoId}] to reel [${reelCommand.reelId}]"

        handleMultipleCommandRequest([reelCommand, videoCommand]) {
            reelVideoManagementService.addVideo(reelCommand.reelId, videoCommand.videoId)
            render(status: SC_CREATED)
        }
    }

    @Secured(["#oauth2.isUser() and #oauth2.hasScope('reels-write')"])
    def removeVideo(ReelCommand reelCommand, VideoCommand videoCommand) {
        log.debug "Removing video [${videoCommand.videoId}] from reel [${reelCommand.reelId}]"

        handleMultipleCommandRequest([reelCommand, videoCommand]) {
            reelVideoManagementService.removeVideo(reelCommand.reelId, videoCommand.videoId)
            render(status: SC_OK)
        }
    }
}
