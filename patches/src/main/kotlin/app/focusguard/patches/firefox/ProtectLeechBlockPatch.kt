@file:Suppress("DEPRECATION")

package app.focusguard.patches.firefox

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

/**
 * Adds friction to removing LeechBlock NG from Firefox Android 160.0.
 *
 * The fingerprints intentionally use the concrete Firefox GeckoEngine methods and their
 * complete parameter descriptors. If Mozilla changes those signatures, patching fails rather
 * than silently modifying a different method.
 *
 * This is not a tamper-proof lock: clearing Firefox data, replacing the APK, or uninstalling
 * Firefox can still remove the extension and this patch.
 */
@Suppress("unused")
val protectLeechBlockPatch = bytecodePatch(
    name = "Protect LeechBlock NG",
    description = "Rejects Firefox's disable and uninstall requests for LeechBlock NG only.",
    default = true,
) {
    compatibleWith("org.mozilla.firefox"("160.0"))

    execute {
        val disableFingerprint = Fingerprint(
            definingClass = "Lmozilla/components/browser/engine/gecko/GeckoEngine;",
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
            definingClass = "Lmozilla/components/browser/engine/gecko/GeckoEngine;",
            name = "uninstallWebExtension",
            returnType = "V",
            parameters = listOf(
                "Lmozilla/components/concept/engine/webextension/WebExtension;",
                "Lmozilla/components/feature/addons/AddonManager\$\$ExternalSyntheticLambda10;",
                "Lmozilla/components/feature/addons/AddonManager\$\$ExternalSyntheticLambda11;",
            ),
        )

        // Resolve both fingerprints before mutating either method. This makes version drift
        // fail the patch as a whole instead of leaving a partially-applied edit in memory.
        val disableMethod = disableFingerprint.method
        val uninstallMethod = uninstallFingerprint.method

        disableMethod.addInstructions(
            0,
            """
                iget-object v0, p1, Lmozilla/components/concept/engine/webextension/WebExtension;->id:Ljava/lang/String;
                const-string/jumbo v1, "leechblockng@proginosko.com"
                invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :leechblock_protection_disable_continue
                new-instance v0, Ljava/lang/IllegalStateException;
                const-string/jumbo v1, "LeechBlock NG is protected by Focus Guard"
                invoke-direct {v0, v1}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V
                invoke-interface {p4, v0}, Lkotlin/jvm/functions/Function1;->invoke(Ljava/lang/Object;)Ljava/lang/Object;
                return-void
                :leechblock_protection_disable_continue
            """.trimIndent(),
        )

        uninstallMethod.addInstructions(
            0,
            """
                iget-object v0, p1, Lmozilla/components/concept/engine/webextension/WebExtension;->id:Ljava/lang/String;
                const-string/jumbo v1, "leechblockng@proginosko.com"
                invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :leechblock_protection_uninstall_continue
                iget-object v1, p1, Lmozilla/components/concept/engine/webextension/WebExtension;->id:Ljava/lang/String;
                new-instance v0, Ljava/lang/IllegalStateException;
                const-string/jumbo v2, "LeechBlock NG is protected by Focus Guard"
                invoke-direct {v0, v2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V
                invoke-interface {p3, v1, v0}, Lkotlin/jvm/functions/Function2;->invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                return-void
                :leechblock_protection_uninstall_continue
            """.trimIndent(),
        )
    }
}
