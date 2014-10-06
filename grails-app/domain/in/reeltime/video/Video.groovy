package in.reeltime.video

import in.reeltime.user.User
import in.reeltime.playlist.Playlist
import in.reeltime.playlist.PlaylistUri
import in.reeltime.reel.Reel

class Video {

    String title
    String description

    String masterPath
    boolean available

    static searchable = {
        only = ['title']
    }

    static belongsTo = [creator: User]

    static hasMany = [
            playlists: Playlist,
            playlistUris: PlaylistUri
    ]

    static constraints = {
        creator nullable: false
        title nullable: false, blank: false
        description nullable: true, blank: true
        masterPath nullable: false, blank: false
    }
}
