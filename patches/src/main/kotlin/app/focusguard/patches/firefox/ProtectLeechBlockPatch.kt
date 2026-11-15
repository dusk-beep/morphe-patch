
@file:Suppress("DEPRECATION")

package app.focusguard.patches.firefox

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.cloneMutable
import app.morphe.util.transformMethods

@Suppress("unused")
val protectLeechBlockPatch = bytecodePatch(
    name = "Protect LeechBlock NG",
    description = "Protects LeechBlock NG from disabling, uninstalling and losing private browsing access.",
    default = true,
) {
    compatibleWith(
        Compatibility(
            packageName = "org.mozilla.firefox",
            name = "Firefox",
            targets = listOf(AppTarget(version = "160.0")),
        )
    )

    execute {
        val engineClassName =
            "Lmozilla/components/browser/engine/gecko/GeckoEngine;"

        val disableFingerprint = Fingerprint(
            definingClass = engineClassName,
            name = "disableWebExtension",
            returnType = "V",
            parameters = listOf(
                "Lmozilla/components/concept/engine/webextension/WebExtension;",
                "Lmozilla/components/concept/engine/webextension/EnableSource;",
                "Lmozilla/components/feature/addons/AddonManager\$\$ExternalSyntheticLambda4;",
                "Lmozilla/components/feature/addons/AddonManager\$\$ExternalSyntheticLambda5;",
            ),
        )

        val uninstallFingerprint = Fingerprint(
            definingClass = engineClassName,
            name = "uninstallWebExtension",
            returnType = "V",
            parameters = listOf(
                "Lmozilla/components/concept/engine/webextension/WebExtension;",
                "Lmozilla/components/feature/addons/AddonManager\$\$ExternalSyntheticLambda10;",
                "Lmozilla/components/feature/addons/AddonManager\$\$ExternalSyntheticLambda11;",
            ),
        )

        val privateBrowsingFingerprint = Fingerprint(
            definingClass = engineClassName,
            name = "setAllowedInPrivateBrowsing",
            returnType = "V",
            parameters = listOf(
                "Lmozilla/components/concept/engine/webextension/WebExtension;",
                "Z",
                "Lmozilla/components/feature/addons/AddonManager\$\$ExternalSyntheticLambda8;",
                "Lmozilla/components/feature/addons/AddonManager\$\$ExternalSyntheticLambda9;",
            ),
        )

        // Resolve all original methods before replacing any.
        val originalDisable = disableFingerprint.method
        val originalUninstall = uninstallFingerprint.method
        val originalPrivateBrowsing = privateBrowsingFingerprint.method

        var protectedDisable: MutableMethod? = null
        var protectedUninstall: MutableMethod? = null
        var protectedPrivateBrowsing: MutableMethod? = null

        val engineClass = mutableClassDefBy(engineClassName)

        engineClass.transformMethods {
            when {
                name == originalDisable.name &&
                    parameters == originalDisable.parameters &&
                    returnType == originalDisable.returnType -> {
                    cloneMutable(additionalRegisters = 7).also {
                        protectedDisable = it
                    }
                }

                name == originalUninstall.name &&
                    parameters == originalUninstall.parameters &&
                    returnType == originalUninstall.returnType -> {
                    cloneMutable(additionalRegisters = 6).also {
                        protectedUninstall = it
                    }
                }

                name == originalPrivateBrowsing.name &&
                    parameters == originalPrivateBrowsing.parameters &&
                    returnType == originalPrivateBrowsing.returnType -> {
                    cloneMutable(additionalRegisters = 2).also {
                        protectedPrivateBrowsing = it
                    }
                }

                else -> this
            }
        }

        val disableMethod = requireNotNull(protectedDisable) {
            "Could not replace disableWebExtension"
        }

        val uninstallMethod = requireNotNull(protectedUninstall) {
            "Could not replace uninstallWebExtension"
        }

        val privateBrowsingMethod = requireNotNull(protectedPrivateBrowsing) {
            "Could not replace setAllowedInPrivateBrowsing"
        }

        // Protect disableWebExtension.
        disableMethod.addInstructions(
            0,
            """
                iget-object v6, p1, Lmozilla/components/concept/engine/webextension/WebExtension;->id:Ljava/lang/String;
                const-string/jumbo v7, "leechblockng@proginosko.com"
                invoke-virtual {v6, v7}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v6
                if-eqz v6, :leechblock_disable_continue

                new-instance v6, Ljava/lang/IllegalStateException;
                const-string/jumbo v7, "LeechBlock NG is protected by Focus Guard"
                invoke-direct {v6, v7}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V
                invoke-interface {p4, v6}, Lkotlin/jvm/functions/Function1;->invoke(Ljava/lang/Object;)Ljava/lang/Object;
                return-void

                :leechblock_disable_continue
            """.trimIndent(),
        )

        // Protect uninstallWebExtension.
        uninstallMethod.addInstructions(
            0,
            """
                iget-object v5, p1, Lmozilla/components/concept/engine/webextension/WebExtension;->id:Ljava/lang/String;
                const-string/jumbo v6, "leechblockng@proginosko.com"
                invoke-virtual {v5, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v5
                if-eqz v5, :leechblock_uninstall_continue

                new-instance v5, Ljava/lang/IllegalStateException;
                const-string/jumbo v6, "LeechBlock NG is protected by Focus Guard"
                invoke-direct {v5, v6}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V
                invoke-interface {p3, p1, v5}, Lkotlin/jvm/functions/Function2;->invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                return-void

                :leechblock_uninstall_continue
            """.trimIndent(),
        )

        // Force private browsing permission to remain enabled.
        // v0 and v1 are additional local registers.
        privateBrowsingMethod.addInstructions(
            0,
            """
                iget-object v0, p1, Lmozilla/components/concept/engine/webextension/WebExtension;->id:Ljava/lang/String;
                const-string/jumbo v1, "leechblockng@proginosko.com"
                invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :leechblock_private_continue

                const/4 p2, 0x1

                :leechblock_private_continue
            """.trimIndent(),
        )
    }
}

