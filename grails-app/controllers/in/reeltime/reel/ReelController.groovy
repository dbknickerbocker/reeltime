package in.reeltime.reel

import in.reeltime.common.AbstractController
import in.reeltime.exceptions.AuthorizationException
import in.reeltime.exceptions.InvalidReelNameException
import in.reeltime.exceptions.ReelNotFoundException
import in.reeltime.exceptions.UserNotFoundException
import in.reeltime.exceptions.VideoNotFoundException

import static in.reeltime.reel.ListMarshaller.*
import static in.reeltime.common.ContentTypes.APPLICATION_JSON
import static javax.servlet.http.HttpServletResponse.*

class ReelController extends AbstractController {

    def reelService
    def reelVideoManagementService

    def listReels(String username) {
        log.debug "Listing reels for user [$username]"
        handleSingleParamRequest(username, 'reel.username.required') {
            def reels = reelService.listReels(username)
            render(status: SC_OK, contentType: APPLICATION_JSON) {
                marshallReelList(reels)
            }
        }
    }

    def addReel(String name) {
        log.debug "Adding reel [$name]"
        handleSingleParamRequest(name, 'reel.name.required') {
            reelService.addReel(name)
            render(status: SC_CREATED)
        }
    }

    def deleteReel(Long reelId) {
        log.debug "Deleting reel [$reelId]"
        handleSingleParamRequest(reelId, 'reel.id.required') {
            reelService.deleteReel(reelId)
            render(status: SC_OK)
        }
    }

    def listVideos(Long reelId) {
        log.debug "Listing videos in reel [$reelId]"
        handleSingleParamRequest(reelId, 'reel.id.required') {
            def videos = reelVideoManagementService.listVideos(reelId)
            render(status: SC_OK, contentType: APPLICATION_JSON) {
                marshallVideoList(videos)
            }
        }
    }

    def addVideo(Long reelId, Long videoId) {
        log.debug "Adding video [$videoId] to reel [$reelId]"
        if(!reelId) {
            errorMessageResponse('reel.id.required', SC_BAD_REQUEST)
        }
        else if(!videoId) {
            errorMessageResponse('video.id.required', SC_BAD_REQUEST)
        }
        else {
            reelVideoManagementService.addVideo(reelId, videoId)
            render(status: SC_CREATED)
        }
    }

    def removeVideo(Long reelId, Long videoId) {
        log.debug "Removing video [$videoId] from reel [$reelId]"
        if(!reelId) {
            errorMessageResponse('reel.id.required', SC_BAD_REQUEST)
        }
        else if(!videoId) {
            errorMessageResponse('video.id.required', SC_BAD_REQUEST)
        }
        else {
            reelVideoManagementService.removeVideo(reelId, videoId)
        }
    }

    private void handleSingleParamRequest(Object paramToCheck, String errorMessageCode, Closure action) {
        paramToCheck ? action() : errorMessageResponse(errorMessageCode, SC_BAD_REQUEST)
    }

    def handleAuthorizationException(AuthorizationException e) {
        exceptionErrorMessageResponse(e, 'reel.unauthorized', SC_FORBIDDEN)
    }

    def handleUserNotFoundException(UserNotFoundException e) {
        exceptionErrorMessageResponse(e, 'reel.unknown.username', SC_BAD_REQUEST)
    }

    def handleReelNotFoundException(ReelNotFoundException e) {
        exceptionErrorMessageResponse(e, 'reel.unknown', SC_BAD_REQUEST)
    }

    def handleInvalidReelNameException(InvalidReelNameException e) {
        exceptionErrorMessageResponse(e, 'reel.invalid.name', SC_BAD_REQUEST)
    }

    def handleVideoNotFoundException(VideoNotFoundException e) {
        exceptionErrorMessageResponse(e, 'video.unknown', SC_BAD_REQUEST)
    }
}
