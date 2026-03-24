/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package gg.essential.util;

//#if FORGE && MC >= 1.16 && MC < 1.21.6
import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformingClassLoader;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

//#if MC >= 1.17
//$$ import cpw.mods.jarhandling.SecureJar;
//$$ import cpw.mods.modlauncher.api.NamedPath;
//$$ import java.util.List;
//#endif

/**
 * Workaround to fix <a href="https://github.com/MinecraftForge/EventBus/issues/44">this issue</a>
 * using unsafe hacks by wrapping the `eventbus` LaunchPluginService and always setting the context class loader.
 * <br>
 * Note that both upstream fixes referenced in the above issue are insufficient to fix the problem.
 * The context class loader is still preferred, and the ForkJoinPool has that set to the system class loader by default.
 */
public class WorkaroundBrokenEventSubclassTransformer {
    public static void apply() {
        try {
            doApply();
        } catch (Exception e) {
            LoggerFactory.getLogger(WorkaroundBrokenEventSubclassTransformer.class)
                    .error("Failed to apply EventSubclassTransformer workaround:", e);
        }
    }

    private static void doApply() {
        UnsafeHacks.Accessor<Launcher, LaunchPluginHandler> launchPluginsField =
            UnsafeHacks.makeAccessor(Launcher.class, "launchPlugins");
        UnsafeHacks.Accessor<LaunchPluginHandler, Map<String, ILaunchPluginService>> pluginsField =
            UnsafeHacks.makeAccessor(LaunchPluginHandler.class, "plugins");

        pluginsField.update(launchPluginsField.get(Launcher.INSTANCE), oldPlugins -> {
            Map<String, ILaunchPluginService> newPlugins = new LinkedHashMap<>(oldPlugins);
            ILaunchPluginService eventbus = newPlugins.get("eventbus");
            if (eventbus != null) {
                newPlugins.put("eventbus", new LaunchPluginServiceWrappedWithContextClassLoader(eventbus));
            }
            return newPlugins;
        });
    }

    public static class LaunchPluginServiceWrappedWithContextClassLoader implements ILaunchPluginService {
        private final ILaunchPluginService inner;
        private final ClassLoader gameClassLoader = Thread.currentThread().getContextClassLoader();
        {
            if (!(gameClassLoader instanceof TransformingClassLoader)) {
                throw new IllegalStateException("Must be initialized while the context class loader is set correctly!");
            }
        }

        public LaunchPluginServiceWrappedWithContextClassLoader(ILaunchPluginService inner) {
            this.inner = inner;
        }

        @Override
        public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
            Thread thread = Thread.currentThread();
            ClassLoader orgContextClassLoader = thread.getContextClassLoader();
            try {
                thread.setContextClassLoader(gameClassLoader);
                return inner.processClass(phase, classNode, classType);
            } finally {
                thread.setContextClassLoader(orgContextClassLoader);
            }
        }

        @Override
        public boolean processClass(Phase phase, ClassNode classNode, Type classType, String reason) {
            Thread thread = Thread.currentThread();
            ClassLoader orgContextClassLoader = thread.getContextClassLoader();
            try {
                thread.setContextClassLoader(gameClassLoader);
                return inner.processClass(phase, classNode, classType, reason);
            } finally {
                thread.setContextClassLoader(orgContextClassLoader);
            }
        }

        @Override
        public int processClassWithFlags(Phase phase, ClassNode classNode, Type classType, String reason) {
            Thread thread = Thread.currentThread();
            ClassLoader orgContextClassLoader = thread.getContextClassLoader();
            try {
                thread.setContextClassLoader(gameClassLoader);
                return inner.processClassWithFlags(phase, classNode, classType, reason);
            } finally {
                thread.setContextClassLoader(orgContextClassLoader);
            }
        }

        @Override
        public String name() {
            return inner.name();
        }

        @Override
        public EnumSet<Phase> handlesClass(Type type, boolean b) {
            return inner.handlesClass(type, b);
        }

        @Override
        public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty, String reason) {
            return inner.handlesClass(classType, isEmpty, reason);
        }

        @Override
        public void offerResource(Path resource, String name) {
            inner.offerResource(resource, name);
        }

        //#if MC >= 1.17
        //$$ @Override
        //$$ public void addResources(List<SecureJar> resources) {
        //$$     inner.addResources(resources);
        //$$ }
        //$$
        //$$ @Override
        //$$ public void initializeLaunch(ITransformerLoader transformerLoader, NamedPath[] specialPaths) {
        //$$     inner.initializeLaunch(transformerLoader, specialPaths);
        //$$ }
        //#endif

        @Override
        public <T> T getExtension() {
            return inner.getExtension();
        }

        @Override
        public void customAuditConsumer(String className, Consumer<String[]> auditDataAcceptor) {
            inner.customAuditConsumer(className, auditDataAcceptor);
        }
    }
}
//#endif
