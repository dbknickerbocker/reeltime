package in.reeltime.user

class UserFollowing {

    User follower
    User followee

    static constraints = {
        follower nullable: false
        followee nullable: false, validator: { val, obj -> val != obj.follower }
    }
}
