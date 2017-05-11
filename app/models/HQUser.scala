package models

import java.util.UUID

import be.objectify.deadbolt.scala.models.{ Permission, Role, Subject }
import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }

/**
 * Created by henri on 5/11/2017.
 */
case class HQUser(
  userID: UUID,
  loginInfo: LoginInfo,
  nickname: String,
  email: String,
  avatarURL: Option[String],
  activated: Boolean,
  extraRoles: List[Role]) extends Identity with Subject {

  override def identifier: String = userID.toString

  override def roles: List[Role] = Roles.User :: extraRoles

  override def permissions: List[Permission] = Nil
}
