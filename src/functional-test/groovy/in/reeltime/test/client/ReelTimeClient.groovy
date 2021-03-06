package in.reeltime.test.client

import grails.plugins.rest.client.RestResponse
import in.reeltime.test.rest.AuthorizationAwareRestClient
import in.reeltime.hls.playlist.MediaPlaylist
import in.reeltime.hls.playlist.VariantPlaylist
import in.reeltime.hls.playlist.parser.MediaPlaylistParser
import in.reeltime.hls.playlist.parser.VariantPlaylistParser
import org.grails.web.json.JSONElement

class ReelTimeClient {

    @Delegate
    private AuthorizationAwareRestClient restClient

    private ReelTimeRequestFactory requestFactory

    // Video creation completion polling defaults
    private static final DEFAULT_MAX_POLL_COUNT = 20
    private static final DEFAULT_RETRY_DELAY_IN_MILLIS = 10 * 1000

    ReelTimeClient(AuthorizationAwareRestClient restClient, ReelTimeRequestFactory requestFactory) {
        this.restClient = restClient
        this.requestFactory = requestFactory
    }

    RestResponse registerUser(String username, String password, String clientName, String displayName) {
        def email = EmailFormatter.emailForUsername(username)

        def request = requestFactory.registerUser(username, password, displayName, email, clientName)
        def response = post(request)

        assertStatusOrFail(response, 201, "Failed to register user [$username].")

        return response
    }

    void removeAccount(String token) {
        def request = requestFactory.removeAccount(token)

        def response = delete(request)
        assertStatusOrFail(response, 200, "Failed to remove account")

        response = delete(request)
        assertStatusOrFail(response, 401, "The token associated with the deleted account is still valid!")
    }

    void sendResetPasswordEmail(String username) {
        def request = requestFactory.sendResetPasswordEmail(username)
        def response = post(request)

        assertStatusOrFail(response, 200, "Failed to send reset password email.")
    }

    void resetPasswordForRegisteredClient(String username, String newPassword, String resetCode,
                                          String clientId, String clientSecret) {
        def request = requestFactory.resetPasswordForRegisteredClient(username, newPassword, resetCode, clientId, clientSecret)
        def response = post(request)

        assertStatusOrFail(response, 200, "Failed to reset password for registered client.")
    }

    RestResponse resetPasswordForNewClient(String username, String newPassword, String resetCode, String clientName) {
        def request = requestFactory.resetPasswordForNewClient(username, newPassword, resetCode, clientName)
        def response = post(request)

        assertStatusOrFail(response, 201, "Failed to reset password for new client.")

        return response
    }

    JSONElement registerNewClient(String username, String password, String clientName) {
        def request = requestFactory.registerClient(username, password, clientName)
        def response = post(request)

        assertStatusOrFail(response, 201, "Failed to register new client.")

        return response.json
    }

    JSONElement listClients(String token) {
        def request = requestFactory.listClients(token)
        def response = get(request)

        assertStatusOrFail(response, 200, "Failed to list clients.")

        return response.json
    }

    void confirmAccount(String token, String confirmationCode) {
        def request = requestFactory.confirmAccount(token, confirmationCode)
        def response = post(request)

        assertStatusOrFail(response, 200, "Failed to confirm account.")
    }

    void confirmAccountForUser(String token, String username) {
        def request = requestFactory.confirmAccountForUser(token, username)
        def response = post(request)

        assertStatusOrFail(response, 200, "Failed to confirm account on user's behalf.")
    }

    void changeDisplayName(String token, String newDisplayName) {
        def request = requestFactory.changeDisplayName(token, newDisplayName)
        def response = post(request)

        assertStatusOrFail(response, 200, "Failed to change display name.")
    }

    void changePassword(String token, String newPassword) {
        def request = requestFactory.changePassword(token, newPassword)
        def response = post(request)

        assertStatusOrFail(response, 200, "Failed to change password.")
    }

    void resetPasswordForUser(String token, String username, String newPassword) {
        def request = requestFactory.resetPasswordForUser(token, username, newPassword)
        def response = post(request)

        assertStatusOrFail(response, 200, "Failed to reset password on user's behalf.")
    }

    JSONElement newsfeed(String token, Integer page = null) {
        def request = requestFactory.newsfeed(token, page)
        def response = get(request)

        assertStatusOrFail(response, 200, "Failed to retrieve newsfeed.")

        return response.json
    }

    JSONElement userProfile(String token, String username) {
        def request = requestFactory.userProfile(token, username)
        def response = get(request)

        assertStatusOrFail(response, 200, "Failed to get user details.")

        return response.json
    }

    JSONElement listUsers(String token, Integer page = null) {
        def request = requestFactory.listUsers(token, page)
        def response = get(request)

        assertStatusOrFail(response, 200, "Failed to retrieve list of users.")

        return response.json
    }

    void followUser(String token, String username) {
        def request = requestFactory.followUser(token, username)
        def response = post(request)

        assertStatusOrFail(response, 201, "Failed to follow user: $username.")
    }

    void unfollowUser(String token, String username) {
        def request = requestFactory.unfollowUser(token, username)
        def response = delete(request)

        assertStatusOrFail(response, 200, "Failed to unfollow user: $username.")
    }

    JSONElement listFollowers(String token, String username, int page = 1) {
        def request = requestFactory.listFollowers(token, username, page)
        def response = get(request)

        assertStatusOrFail(response, 200, "Failed to list followers for: $username.")

        return response.json
    }

    JSONElement listFollowees(String token, String username, int page = 1) {
        def request = requestFactory.listFollowees(token, username, page)
        def response = get(request)

        assertStatusOrFail(response, 200, "Failed to list followees for: $username.")

        return response.json
    }

    JSONElement listVideos(String token, Integer page = null) {
        def request = requestFactory.listVideos(token, page)
        def response = get(request)

        assertStatusOrFail(response, 200, "Failed to retrieve list of videos.")

        return response.json
    }

    Long uploadVideoToUncategorizedReel(String token, String title = 'minimum-viable-video') {
        uploadVideoToReel(token, 'Uncategorized', title)
    }

    Long uploadVideoToReel(String token, String reel, String title) {
        def video = new File('src/test/resources/files/videos/small.mp4')
        def thumbnail = new File('src/test/resources/files/images/small.png')

        def request = requestFactory.uploadVideo(token, title, reel, video, thumbnail)
        def response = post(request)

        assertStatusOrFail(response, 202, "Failed to upload video.")

        long videoId = response.json.video_id
        pollForCreationComplete(token, videoId)
        return videoId
    }

    private int pollForCreationComplete(String uploadToken, long videoId,
              int maxPollCount = DEFAULT_MAX_POLL_COUNT, long retryDelayMillis = DEFAULT_RETRY_DELAY_IN_MILLIS) {

        def request = requestFactory.getVideo(uploadToken, videoId)

        int videoCreatedStatus = 0
        int pollCount = 0

        while(videoCreatedStatus != 200 && pollCount < maxPollCount) {
            def response = get(request)
            videoCreatedStatus = response.status

            if(videoCreatedStatus == 202) {
                println "Video [$videoId] is still being created. Sleeping for 5 seconds before next status query."
                sleep(retryDelayMillis)
            }
            pollCount++
        }

        assert videoCreatedStatus == 200 : "Exceeded polling limit for uploading video."
        return videoCreatedStatus
    }

    VariantPlaylist variantPlaylist(String token, Long videoId) {
        def request = requestFactory.variantPlaylist(token, videoId)
        def response = get(request)

        assertStatusOrFail(response, 200, "Failed to retrieve variant playlist for video [$videoId].")

        def baos = new ByteArrayInputStream(response.body as byte[])
        return VariantPlaylistParser.parse(baos.newReader())
    }

    MediaPlaylist mediaPlaylist(String token, Long videoId, Long playlistId) {
        def request = requestFactory.mediaPlaylist(token, videoId, playlistId)
        def response = get(request)

        assertStatusOrFail(response, 200, "Failed to retrieve media playlist [$playlistId] for video [$videoId].")

        def baos = new ByteArrayInputStream(response.body as byte[])
        return MediaPlaylistParser.parse(baos.newReader())
    }

    InputStream mediaSegment(String token, Long videoId, Long playlistId, Long segmentId) {
        def request = requestFactory.segment(token, videoId, playlistId, segmentId)
        def response = get(request)

        assertStatusOrFail(response, 200, "Failed to retrieve segment [$segmentId] for playlist [$playlistId] belonging to video [$videoId].")

        return new ByteArrayInputStream(response.body as byte[])
    }

    // TODO: Update existing invocations to specify username
    Long getUncategorizedReelId(String token, String username = 'bob') {
        def request = requestFactory.listReelsForUser(token, username)
        def response = get(request)

        assertStatusOrFail(response, 200, "Failed to list reels.")

        def uncategorizedReel = response.json.reels.find { it.name == 'Uncategorized' }
        return uncategorizedReel.reel_id
    }

    JSONElement listReels(String token, Integer page = null) {
        def request = requestFactory.listReels(token, page)
        def response = get(request)

        assertStatusOrFail(response, 200, "Failed to retrieve list of reels.")

        return response.json
    }

    JSONElement getReel(String token, Long reelId) {
        def request = requestFactory.getReel(token, reelId)
        def response = get(request)

        assertStatusOrFail(response, 200, "Failed to get reel [$reelId].")

        return response.json
    }

    Long addReel(String token, String reelName) {
        def request = requestFactory.addReel(token, reelName)
        def response = post(request)

        assertStatusOrFail(response, 201, "Failed to add reel [$reelName].")

        return response.json.reel_id
    }

    void deleteReel(String token, Long reelId) {
        def request = requestFactory.deleteReel(token, reelId)
        def response = delete(request)

        assertStatusOrFail(response, 200, "Failed to delete reel [$reelId].")
    }

    void addVideoToReel(String token, Long reelId, Long videoId) {
        def request = requestFactory.addVideoToReel(token, reelId, videoId)
        def response = post(request)

        assertStatusOrFail(response, 201, "Failed to add video [$videoId] to reel [$reelId].")
    }

    JSONElement listVideosInReel(String token, Long reelId) {
        def request = requestFactory.listVideosInReel(token, reelId)
        def response = get(request)

        assertStatusOrFail(response, 200, "Failed to list videos in reel [$reelId].")

        return response.json
    }

    JSONElement listAudienceMembers(String token, Long reelId) {
        def request = requestFactory.listAudienceMembers(token, reelId)
        def response = get(request)

        assertStatusOrFail(response, 200, "Failed to list audience members for reel [$reelId].")

        return response.json
    }

    void addAudienceMember(String token, Long reelId) {
        def request = requestFactory.addAudienceMember(token, reelId)
        def response = post(request)

        assertStatusOrFail(response, 201, "Failed to add audience member for reel [$reelId].")
    }

    void removeAudienceMember(String token, Long reelId) {
        def request = requestFactory.removeAudienceMember(token, reelId)
        def response = delete(request)

        assertStatusOrFail(response, 200, "Failed to remove audience member for reel [$reelId].")
    }

    private assertStatusOrFail(RestResponse response, int expectedStatus, String message) {
        assert response.status == expectedStatus : getFailureText(response, message)
    }

    private static getFailureText(RestResponse response, String message) {
        message + ' ' + getStatusAndJsonText(response)
    }
    private static getStatusAndJsonText(RestResponse response) {
        "Status code: ${response.status}. JSON: ${response.json}"
    }
}
