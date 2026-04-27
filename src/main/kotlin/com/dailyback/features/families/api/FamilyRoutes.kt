package com.dailyback.features.families.api

import com.dailyback.features.families.application.FamilyMemberPermissionService
import com.dailyback.features.families.application.FamilyPermissionAuthorizer
import com.dailyback.features.families.application.FamilyService
import com.dailyback.features.families.domain.DuplicateFamilyMemberInviteException
import com.dailyback.features.families.domain.FamilyMemberNotFoundException
import com.dailyback.features.families.domain.FamilyNotFoundException
import com.dailyback.features.families.domain.InvalidFamilyMemberDisplayNameException
import com.dailyback.features.families.domain.InvalidFamilyMemberRoleChangeException
import com.dailyback.features.families.domain.InvalidFamilyMemberRoleValueException
import com.dailyback.features.families.domain.InvalidFamilyNameException
import com.dailyback.features.families.domain.InviteTargetInAnotherFamilyException
import com.dailyback.features.families.domain.LastFamilyAdminException
import com.dailyback.features.families.domain.NoFamilyForUserException
import com.dailyback.features.families.domain.NotFamilyAdminException
import com.dailyback.features.families.domain.UserAlreadyHasFamilyException
import com.dailyback.features.users.domain.UserNotFoundException
import com.dailyback.shared.api.requireJwtUserId
import com.dailyback.shared.api.requireJwtUserIdAndPermission
import com.dailyback.shared.domain.family.FamilyPermissionKey
import com.dailyback.shared.api.toUuidOrBadRequest
import com.dailyback.shared.errors.ApiException
import com.dailyback.shared.errors.ErrorCodes
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.familyRoutes(
    familyService: FamilyService,
    familyMemberPermissionService: FamilyMemberPermissionService,
    familyPermissionAuthorizer: FamilyPermissionAuthorizer,
) {
    route("/families") {
        post {
            val userId = call.requireJwtUserId()
            val request = call.receive<CreateFamilyRequest>()
            val family = runCatching {
                familyService.createFamily(actorUserId = userId, rawName = request.name)
            }.getOrElse { throw mapFamilyException(it) }
            call.respond(HttpStatusCode.Created, family.toResponse())
        }

        get("/me") {
            val userId = call.requireJwtUserId()
            val family = runCatching {
                familyService.getMyFamily(actorUserId = userId)
            }.getOrElse { throw mapFamilyException(it) }
            call.respond(family.toResponse())
        }

        get("/me/members") {
            val userId = call.requireJwtUserId()
            val members = runCatching {
                familyService.listMyFamilyMembers(actorUserId = userId).map { it.toResponse() }
            }.getOrElse { throw mapFamilyException(it) }
            call.respond(members)
        }

        post("/current/members") {
            val userId = call.requireJwtUserIdAndPermission(
                familyPermissionAuthorizer,
                FamilyPermissionKey.CAN_INVITE_MEMBERS,
            )
            val request = call.receive<InviteFamilyMemberRequest>()
            val member = runCatching {
                familyService.inviteMember(
                    actorUserId = userId,
                    displayName = request.displayName,
                    rawDocument = request.document,
                    rawEmail = request.email,
                    rawPhone = request.phone,
                )
            }.getOrElse { throw mapFamilyException(it) }
            call.respond(HttpStatusCode.Created, member.toResponse())
        }

        patch("/current/members/{memberId}/role") {
            val userId = call.requireJwtUserIdAndPermission(
                familyPermissionAuthorizer,
                FamilyPermissionKey.CAN_MANAGE_MEMBERS,
            )
            val memberId = call.parameters["memberId"].toUuidOrBadRequest("memberId")
            val request = call.receive<UpdateFamilyMemberRoleRequest>()
            val member = runCatching {
                familyService.updateMemberRole(
                    actorUserId = userId,
                    targetMemberId = memberId,
                    rawRole = request.role,
                )
            }.getOrElse { throw mapFamilyException(it) }
            call.respond(member.toResponse())
        }

        delete("/current/members/{memberId}") {
            val userId = call.requireJwtUserIdAndPermission(
                familyPermissionAuthorizer,
                FamilyPermissionKey.CAN_MANAGE_MEMBERS,
            )
            val memberId = call.parameters["memberId"].toUuidOrBadRequest("memberId")
            runCatching {
                familyService.removeMember(actorUserId = userId, targetMemberId = memberId)
            }.getOrElse { throw mapFamilyException(it) }
            call.respond(HttpStatusCode.NoContent)
        }

        get("/current/members/{memberId}/permissions") {
            val userId = call.requireJwtUserId()
            val memberId = call.parameters["memberId"].toUuidOrBadRequest("memberId")
            val permissions = runCatching {
                familyMemberPermissionService.getMemberPermissions(
                    actorUserId = userId,
                    targetMemberId = memberId,
                )
            }.getOrElse { throw mapFamilyException(it) }
            call.respond(permissions.toResponse())
        }

        get("/current/me/permissions") {
            val userId = call.requireJwtUserId()
            val permissions = runCatching {
                familyMemberPermissionService.getMyPermissions(userId)
            }.getOrElse { throw mapFamilyException(it) }
            call.respond(permissions.toResponse())
        }

        put("/current/members/{memberId}/permissions") {
            val userId = call.requireJwtUserId()
            val memberId = call.parameters["memberId"].toUuidOrBadRequest("memberId")
            val body = call.receive<FamilyMemberPermissionsResponse>()
            val saved = runCatching {
                familyMemberPermissionService.putMemberPermissions(
                    actorUserId = userId,
                    targetMemberId = memberId,
                    flags = body.toFlags(),
                )
            }.getOrElse { throw mapFamilyException(it) }
            call.respond(saved.toResponse())
        }
    }
}

private fun mapFamilyException(cause: Throwable): Throwable = when (cause) {
    is InvalidFamilyNameException -> ApiException(
        statusCode = HttpStatusCode.BadRequest,
        errorCode = ErrorCodes.INVALID_FAMILY_NAME,
        message = cause.message ?: "Invalid family name",
    )

    is UserAlreadyHasFamilyException -> ApiException(
        statusCode = HttpStatusCode.Conflict,
        errorCode = ErrorCodes.USER_ALREADY_HAS_FAMILY,
        message = cause.message ?: "User already belongs to a family",
    )

    is NoFamilyForUserException -> ApiException(
        statusCode = HttpStatusCode.NotFound,
        errorCode = ErrorCodes.NO_FAMILY_FOR_USER,
        message = cause.message ?: "User has no family",
    )

    is FamilyNotFoundException -> ApiException(
        statusCode = HttpStatusCode.NotFound,
        errorCode = ErrorCodes.FAMILY_NOT_FOUND,
        message = cause.message ?: "Family not found",
    )

    is UserNotFoundException -> ApiException(
        statusCode = HttpStatusCode.NotFound,
        errorCode = ErrorCodes.USER_NOT_FOUND,
        message = cause.message ?: "User not found",
    )

    is NotFamilyAdminException -> ApiException(
        statusCode = HttpStatusCode.Forbidden,
        errorCode = ErrorCodes.NOT_FAMILY_ADMIN,
        message = cause.message ?: "Forbidden",
    )

    is DuplicateFamilyMemberInviteException -> ApiException(
        statusCode = HttpStatusCode.Conflict,
        errorCode = ErrorCodes.DUPLICATE_FAMILY_MEMBER_INVITE,
        message = cause.message ?: "Conflict",
    )

    is InviteTargetInAnotherFamilyException -> ApiException(
        statusCode = HttpStatusCode.Conflict,
        errorCode = ErrorCodes.INVITE_TARGET_IN_OTHER_FAMILY,
        message = cause.message ?: "Conflict",
    )

    is InvalidFamilyMemberDisplayNameException -> ApiException(
        statusCode = HttpStatusCode.BadRequest,
        errorCode = ErrorCodes.INVALID_FAMILY_MEMBER_DISPLAY_NAME,
        message = cause.message ?: "Invalid display name",
    )

    is FamilyMemberNotFoundException -> ApiException(
        statusCode = HttpStatusCode.NotFound,
        errorCode = ErrorCodes.FAMILY_MEMBER_NOT_FOUND,
        message = cause.message ?: "Family member not found",
    )

    is LastFamilyAdminException -> ApiException(
        statusCode = HttpStatusCode.Conflict,
        errorCode = ErrorCodes.LAST_FAMILY_ADMIN,
        message = cause.message ?: "Conflict",
    )

    is InvalidFamilyMemberRoleValueException -> ApiException(
        statusCode = HttpStatusCode.BadRequest,
        errorCode = ErrorCodes.INVALID_FAMILY_MEMBER_ROLE_VALUE,
        message = cause.message ?: "Invalid role",
    )

    is InvalidFamilyMemberRoleChangeException -> ApiException(
        statusCode = HttpStatusCode.BadRequest,
        errorCode = ErrorCodes.INVALID_FAMILY_MEMBER_ROLE_CHANGE,
        message = cause.message ?: "Invalid role change",
    )

    else -> cause
}
