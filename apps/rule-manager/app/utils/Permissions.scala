package utils

import com.gu.permissions._
import com.gu.pandomainauth.model.User
import com.gu.typerighter.lib.CommonConfig
import play.api.libs.json.{JsArray, JsBoolean, JsObject, JsString, JsValue}

trait PermissionsHandler {
  def config: CommonConfig

  private val permissionsStage = if (config.stage == "prod") { "PROD" }
  else { "CODE" }
  private val permissions = PermissionsProvider(
    PermissionsConfig(
      permissionsStage,
      config.awsRegion,
      config.awsCredentials,
      config.permissionsBucket
    )
  )

  def storeIsEmpty: Boolean = {
    permissions.storeIsEmpty
  }

  def hasPermission(user: User, permission: PermissionDefinition): Boolean = {
    user match {
      case User(_, _, email, _) => permissions.hasPermission(permission, email)
      case _                    => false
    }
  }

  def userAndPermissionsToJson(user: User, permissions: List[PermissionDefinition]): JsValue = {
    JsObject(
      Seq(
        "permissions" -> JsArray(
          permissions.map(permission =>
            JsObject(
              Seq(
                "permission" -> JsString(permission.name),
                "active" -> JsBoolean(hasPermission(user, permission))
              )
            )
          ).toIndexedSeq
        ),
        "user" -> JsObject(Seq(
            "firstName" -> JsString(user.firstName),
            "lastName" -> JsString(user.lastName),
            "email" -> JsString(user.email),
            "avatarUrl" -> JsString(user.avatarUrl.getOrElse(""))
        ))
      )
    )
  }
}
