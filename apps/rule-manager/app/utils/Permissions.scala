package utils

import com.gu.permissions._

import com.gu.pandomainauth.model.User
import com.gu.typerighter.lib.CommonConfig

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
}
