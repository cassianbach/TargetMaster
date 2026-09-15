package com.targetmaster.mixin;

import com.targetmaster.TextureStorage;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method={"reloadResources(ZLnet/minecraft/client/MinecraftClient$LoadingContext;)Ljava/util/concurrent/CompletableFuture;"}, at={@At(value="HEAD")})
    private void particletuner$ensurePackBeforeReload(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        TextureStorage.ensureEnabled();
    }
}
