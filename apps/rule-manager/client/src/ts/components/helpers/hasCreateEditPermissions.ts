import { Permission } from "../../utils/window";

export const hasCreateEditPermissions = (permissions: Permission[]) => 
    permissions.some(permission => permission.permission === "manage_rules" && permission.active === true)