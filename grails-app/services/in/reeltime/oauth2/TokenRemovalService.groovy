package in.reeltime.oauth2

import in.reeltime.user.User

class TokenRemovalService {

    void removeAllTokensForUser(User user) {
        def clientIds = collectClientIdsForUser(user)

        def accessTokens = findAccessTokens(user, clientIds)
        def refreshTokens = findRefreshTokensFromAccessTokens(accessTokens)

        accessTokens.each { AccessToken accessToken ->
            log.info "Removing access token [${accessToken.id}]"
            accessToken.delete()
        }

        refreshTokens.each { RefreshToken refreshToken ->
            log.info "Removing refresh token [${refreshToken.id}]"
            refreshToken.delete()
        }
    }

    private static Collection<String> collectClientIdsForUser(User user) {
        user.clients.collect { it.clientId }
    }

    private static Collection<AccessToken> findAccessTokens(User user, Collection<String> clientIds) {
        AccessToken.findAll {
            username == user.username || clientIds.contains(clientId)
        }
    }

    private static Collection<RefreshToken> findRefreshTokensFromAccessTokens(Collection<AccessToken> accessTokens) {
        def refreshTokenValues = collectRefreshTokenValuesFromAccessTokens(accessTokens)

        RefreshToken.findAll {
            refreshTokenValues.contains(value)
        }
    }

    private static Collection<String> collectRefreshTokenValuesFromAccessTokens(Collection<AccessToken> accessTokens) {
        def refreshTokenValues = []
        accessTokens.each {
            def refreshToken = it.refreshToken

            if(refreshToken) {
                refreshTokenValues << refreshToken
            }
        }
        return refreshTokenValues
    }
}