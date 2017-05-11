package models.services

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import models.HQUser
import models.daos.HQUserDAO
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

/**
 * Handles actions to users.
 *
 * @param userDAO The user DAO implementation.
 */
class UserServiceImpl @Inject() (userDAO: HQUserDAO) extends UserService {

  /**
   * Retrieves a user that matches the specified ID.
   *
   * @param id The ID to retrieve a user.
   * @return The retrieved user or None if no user could be retrieved for the given ID.
   */
  def retrieve(id: UUID) = userDAO.find(id)

  /**
   * Retrieves a user that matches the specified login info.
   *
   * @param loginInfo The login info to retrieve a user.
   * @return The retrieved user or None if no user could be retrieved for the given login info.
   */
  def retrieve(loginInfo: LoginInfo): Future[Option[HQUser]] = userDAO.find(loginInfo)

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: HQUser) = userDAO.save(user)

  /**
   * Saves the social profile for a user.
   *
   * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
   *
   * @param profile The social profile to save.
   * @return The user for whom the profile was saved.
   */
  // todo ensure there is an email and some sort of name, otherwise error -> rethink UserService trait!!
  def save(profile: CommonSocialProfile) = {
    userDAO.find(profile.loginInfo).flatMap {
      case Some(user) => // Update user with profile
        userDAO.save(user.copy(
          nickname = profile.fullName.getOrElse("anon" + user.identifier),
          email = profile.email.getOrElse("unknown"),
          avatarURL = profile.avatarURL
        ))
      case None => // Insert a new user
        val uuid = UUID.randomUUID()
        userDAO.save(HQUser(
          userID = uuid,
          loginInfo = profile.loginInfo,
          nickname = profile.fullName.getOrElse("anon" + uuid.toString),
          email = profile.email.getOrElse("unknown"),
          avatarURL = profile.avatarURL,
          activated = true,
          extraRoles = Nil
        ))
    }
  }
}
