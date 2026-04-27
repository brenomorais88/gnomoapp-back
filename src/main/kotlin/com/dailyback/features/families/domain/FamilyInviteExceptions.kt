package com.dailyback.features.families.domain

class NotFamilyAdminException : RuntimeException("Only an active family ADMIN can invite members")

class DuplicateFamilyMemberInviteException : RuntimeException("A member or invite with the same identifier already exists in this family")

class InviteTargetInAnotherFamilyException : RuntimeException("This user already belongs to another family")

class InvalidFamilyMemberDisplayNameException : RuntimeException("Display name must not be blank")
