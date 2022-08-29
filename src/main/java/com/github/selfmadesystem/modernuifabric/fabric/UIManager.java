/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.selfmadesystem.modernuifabric.fabric;

import com.github.selfmadesystem.modernuifabric.fabric.mixin.*;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import icyllis.modernui.ModernUI;
import icyllis.modernui.R;
import icyllis.modernui.animation.LayoutTransition;
import icyllis.modernui.annotation.*;
import icyllis.modernui.core.*;
import icyllis.modernui.fragment.*;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.font.GlyphManager;
import icyllis.modernui.graphics.opengl.*;
import icyllis.modernui.lifecycle.*;
import icyllis.modernui.math.Matrix4;
import icyllis.modernui.math.Rect;
import icyllis.modernui.test.TestFragment;
import com.github.selfmadesystem.modernuifabric.testfabric.TestListFragment;
import com.github.selfmadesystem.modernuifabric.testfabric.TestPauseFragment;
import icyllis.modernui.text.*;
import com.github.selfmadesystem.modernuifabric.textmc.ModernStringSplitter;
import com.github.selfmadesystem.modernuifabric.textmc.TextLayoutEngine;
import icyllis.modernui.view.*;
import icyllis.modernui.view.menu.ContextMenuBuilder;
import icyllis.modernui.view.menu.MenuHelper;
import icyllis.modernui.widget.CoordinatorLayout;
import icyllis.modernui.widget.EditText;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.LoadingErrorScreen;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import static icyllis.modernui.ModernUI.LOGGER;
import static icyllis.modernui.graphics.opengl.GLCore.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Manage UI thread and connect Minecraft to Modern UI view system at most bottom level.
 * This class is public only for some hooking methods.
 */
@ApiStatus.Internal
@NotThreadSafe
@Environment(EnvType.CLIENT)
public final class UIManager implements LifecycleOwner {

    // the logger marker
    static final Marker MARKER = MarkerManager.getMarker("UIManager");

    // configs
    static volatile boolean sPlaySoundOnLoaded;

    // the global instance, lazily init
    private static volatile UIManager sInstance;

    private static final int fragment_container = 0x01020007;

    @SuppressWarnings("NoTranslation")
    public static final KeyMapping OPEN_CENTER_KEY = new KeyMapping(
            "key.modernui.openCenter", KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM, GLFW_KEY_K, "Modern UI");

    /*public static final Method SEND_TO_CHAT =
            ObfuscationReflectionHelper.findMethod(ChatComponent.class, "m_93790_",
                    Component.class, int.class, int.class, boolean.class);*/

    /*
    public static final Field BY_PATH =
            ObfuscationReflectionHelper.findField(TextureManager.class, "f_118468_");
    public static final Field TEXTURES_BY_NAME =
            ObfuscationReflectionHelper.findField(TextureAtlas.class, "f_118264_");
    public static final Field MAIN_IMAGE =
            ObfuscationReflectionHelper.findField(TextureAtlasSprite.class, "f_118342_");
    public static final Field IMAGE_PIXELS =
            ObfuscationReflectionHelper.findField(com.mojang.blaze3d.platform.NativeImage.class, "f_84964_");
    public static final Field TEXTURE_ID =
            ObfuscationReflectionHelper.findField(AbstractTexture.class, "f_117950_");
    */

    // minecraft
    private final Minecraft minecraft = Minecraft.getInstance();

    // minecraft window
    private final Window mWindow = minecraft.getWindow();

    private final PoseStack mEmptyPoseStack = new PoseStack();

    // the UI thread
    private final Thread mUiThread;
    private volatile Looper mLooper;
    private volatile boolean mRunning;

    // the view root impl
    private volatile ViewRootImpl mRoot;

    // the top-level view of the window
    private CoordinatorLayout mDecor;
    private FragmentContainerView mFragmentContainerView;


    /// Task Handling \\\

    // elapsed time from a screen open in milliseconds, Render thread
    private long mElapsedTimeMillis;

    // time for drawing, Render thread
    private long mFrameTimeNanos;


    /// Rendering \\\

    // the UI framebuffer
    private GLFramebuffer mFramebuffer;
    GLSurfaceCanvas mCanvas;
    private final Matrix4 mProjectionMatrix = new Matrix4();


    /// User Interface \\\

    // indicates the current Modern UI screen, updated on main thread
    @Nullable
    volatile MuiScreen mScreen;

    private boolean mFirstScreenOpened = false;


    /// Lifecycle \\\

    LifecycleRegistry mFragmentLifecycleRegistry;
    private final OnBackPressedDispatcher mOnBackPressedDispatcher =
            new OnBackPressedDispatcher(() -> minecraft.tell(this::onBackPressed));

    private ViewModelStore mViewModelStore;
    volatile FragmentController mFragmentController;


    /// Input Event \\\

    private int mButtonState;

    private UIManager() {
        // events
        MinecraftForge.EVENT_BUS.register(this);
        MuiFabricApi.addOnScrollListener(this::onScroll);
        MuiFabricApi.addOnScreenChangeListener(this::onScreenChange);
        MuiFabricApi.addOnWindowResizeListener((width, height, guiScale, oldGuiScale) -> resize());

        mUiThread = new Thread(this::run, "UI thread");
        mUiThread.start();

        mRunning = true;
    }

    @RenderThread
    static void initialize() {
        Core.checkRenderThread();
        assert sInstance == null;
        sInstance = new UIManager();
        LOGGER.info(MARKER, "UI manager initialized");
    }

    @RenderThread
    static void initializeRenderer() {
        Core.checkRenderThread();
        assert sInstance != null;
        sInstance.mCanvas = ModernUIFabric.hasGLCapsError() ? null : GLSurfaceCanvas.initialize();
        //glEnable(GL_MULTISAMPLE);
        GLFramebuffer framebuffer = new GLFramebuffer(4);
        if (sInstance.mCanvas != null) {
            framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT0, GL_RGBA8);
            framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT1, GL_RGBA8);
            framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT2, GL_RGBA8);
            framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT3, GL_RGBA8);
            // no depth buffer
            framebuffer.addRenderbufferAttachment(GL_STENCIL_ATTACHMENT, GL_STENCIL_INDEX8);
            framebuffer.setDrawBuffer(GL_COLOR_ATTACHMENT0);
        } else {
            LOGGER.info(MARKER, "Disabled UI renderer");
        }
        sInstance.mFramebuffer = framebuffer;
    }

    @Nonnull
    static UIManager getInstance() {
        // Do not push into stack, since it's lazily init
        if (sInstance == null)
            throw new IllegalStateException("UI manager was never initialized. " +
                    "Please check whether the loader threw an exception before.");
        return sInstance;
    }

    @UiThread
    private void run() {
        init();
        while (true) {
            try {
                Looper.loop();
            } catch (Throwable e) {
                LOGGER.error(MARKER, "An error occurred on UI thread", e);
                // dev can add breakpoints
                if (mRunning && ModernUIFabric.isDeveloperMode()) {
                    continue;
                } else {
                    minecraft.tell(this::dump);
                    minecraft.tell(() -> Minecraft.crash(
                            CrashReport.forThrowable(e, "Exception on UI thread")));
                }
            }
            break;
        }
    }

    /**
     * Schedule UI and create views.
     *
     * @param fragment the main fragment
     * @param callback the user interface callbacks
     */
    @MainThread
    void start(@Nonnull Fragment fragment, @Nullable UICallback callback) {
        if (!minecraft.isSameThread()) {
            throw new IllegalStateException("Not called from main thread");
        }
        minecraft.setScreen(new SimpleScreen(this, fragment, callback));
    }

    @MainThread
    void start(LocalPlayer p, AbstractContainerMenu menu, @Nonnull ResourceLocation key) {
        // internally called, so no explicitly checks
        assert minecraft.isSameThread();
        final OpenMenuEvent event = new OpenMenuEvent(menu);
        ModernUIFabric.post(key.getNamespace(), event);
        final Fragment fragment = event.getFragment();
        if (fragment == null) {
            p.closeContainer(); // close server menu whatever it is
            if (ModernUIFabric.isDeveloperMode()) {
                // only log to devs
                LOGGER.warn(MARKER, "No fragment set, closing menu {}, registry key {}", menu, key);
            }
        } else {
            p.containerMenu = menu;
            minecraft.setScreen(new MenuScreen<>(menu, p.getInventory(), this, fragment, event.getCallback()));
        }
    }

    @MainThread
    void onBackPressed() {
        final MuiScreen screen = mScreen;
        if (screen == null)
            return;
        if (screen.getCallback() != null && !screen.getCallback().shouldClose()) {
            return;
        }
        if (screen instanceof MenuScreen) {
            if (minecraft.player != null) {
                minecraft.player.closeContainer();
            }
        } else {
            minecraft.setScreen(null);
        }
    }

    /**
     * Get elapsed time in UI, update every frame. Internal use only.
     *
     * @return drawing time in milliseconds
     */
    static long getElapsedTime() {
        if (sInstance == null) {
            return Core.timeMillis();
        }
        return sInstance.mElapsedTimeMillis;
    }

    /**
     * Get synced frame time, update every frame
     *
     * @return frame time in nanoseconds
     */
    static long getFrameTimeNanos() {
        if (sInstance == null) {
            return Core.timeNanos();
        }
        return sInstance.mFrameTimeNanos;
    }

    CoordinatorLayout getDecorView() {
        return mDecor;
    }

    @Nonnull
    @Override
    public Lifecycle getLifecycle() {
        // STRONG reference "this"
        return mFragmentLifecycleRegistry;
    }

    // Called when open a screen from Modern UI, or back to the screen
    @MainThread
    void initScreen(@Nonnull MuiScreen screen) {
        if (mScreen != screen) {
            if (mScreen != null) {
                LOGGER.warn(MARKER, "You cannot set multiple screens.");
                removed();
            }
            mRoot.mHandler.post(this::suppressLayoutTransition);
            mFragmentController.getFragmentManager().beginTransaction()
                    .add(fragment_container, screen.getFragment(), "main")
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
            mRoot.mHandler.post(this::restoreLayoutTransition);
        }
        mScreen = screen;
        // ensure it's resized
        resize();
    }

    @UiThread
    void suppressLayoutTransition() {
        LayoutTransition transition = mDecor.getLayoutTransition();
        transition.disableTransitionType(LayoutTransition.APPEARING);
        transition.disableTransitionType(LayoutTransition.DISAPPEARING);
    }

    @UiThread
    void restoreLayoutTransition() {
        LayoutTransition transition = mDecor.getLayoutTransition();
        transition.enableTransitionType(LayoutTransition.APPEARING);
        transition.enableTransitionType(LayoutTransition.DISAPPEARING);
    }

    Screen createCapsErrorScreen() {
        final String glRenderer = glGetString(GL_RENDERER);
        final String glVersion = glGetString(GL_VERSION);
        String extensions = String.join(", ", GLCore.getUnsupportedList());
        return new ConfirmScreen(dontShow -> {
            if (dontShow) {
                Config.CLIENT.showGLCapsError.set(false);
                Config.CLIENT.saveAndReload();
            }
            minecraft.setScreen(null);
        }, Component.translatable("error.modernui.gl_caps"),
                Component.translatable("error.modernui.gl_caps_desc", glRenderer, glVersion, extensions),
                Component.translatable("gui.modernui.dont_show_again"),
                CommonComponents.GUI_CANCEL) {
            @Override
            protected void addButtons(int i) {
                this.addExitButton(new Button(this.width / 2 - 50 - 105, i, 100, 20, this.yesButton,
                        b -> this.callback.accept(true)));
                this.addExitButton(new Button(this.width / 2 - 50, i, 100, 20,
                        Component.translatable("gui.modernui.ok"), b -> Util.getPlatform().openUri(
                        "https://github.com/BloCamLimb/ModernUI/wiki/OpenGL-4.5-support")));
                this.addExitButton(new Button(this.width / 2 - 50 + 105, i, 100, 20, this.noButton,
                        b -> this.callback.accept(false)));
            }
        };
    }

    @SubscribeEvent
    void onGuiOpen(@Nonnull ScreenEvent.Opening event) {
        final Screen next = event.getNewScreen();
        // true if there will be no screen to open
        boolean closeScreen = next == null;

        if (!mFirstScreenOpened && !(next instanceof LoadingErrorScreen)) {
            if (sPlaySoundOnLoaded) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f));
            }
            if (ModernUIFabric.isOptiFineLoaded() &&
                    (ModernUIFabric.getBootstrapLevel() & ModernUIFabric.BOOTSTRAP_DISABLE_TEXT_ENGINE) == 0) {
                OptiFineIntegration.setFastRender(false);
                LOGGER.info(MARKER, "Disabled OptiFine Fast Render");
            }
            if (ModernUIFabric.hasGLCapsError() && Config.CLIENT.showGLCapsError.get()) {
                event.setNewScreen(createCapsErrorScreen());
            }
            mFirstScreenOpened = true;
        }

        if (closeScreen) {
            removed();
            return;
        }

        if (mScreen != next && next instanceof MuiScreen) {
            //mTicks = 0;
            mElapsedTimeMillis = 0;
        }
        if (mScreen != next && mScreen != null) {
            onHoverMove(false);
        }
        // for non-mui screens
        if (mScreen == null && minecraft.screen == null) {
            //mTicks = 0;
            mElapsedTimeMillis = 0;
        }
    }

    private void onScreenChange(@Nullable Screen oldScreen, @Nullable Screen newScreen) {
        BlurHandler.INSTANCE.blur(newScreen);
        if (newScreen == null) {
            removed();
        }
    }

    @UiThread
    private void init() {
        long startTime = System.nanoTime();
        mLooper = Core.initUiThread();

        mRoot = this.new ViewRootImpl();

        mDecor = new CoordinatorLayout();
        // make the root view clickable through, so that views can lose focus
        mDecor.setClickable(true);
        mDecor.setFocusableInTouchMode(true);
        mDecor.setWillNotDraw(true);
        mDecor.setId(R.id.content);
        updateLayoutDir(false);

        mFragmentContainerView = new FragmentContainerView();
        mFragmentContainerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mFragmentContainerView.setWillNotDraw(true);
        mFragmentContainerView.setId(fragment_container);
        mDecor.addView(mFragmentContainerView);

        mDecor.setLayoutTransition(new LayoutTransition());

        mRoot.setView(mDecor);
        resize();

        mDecor.getViewTreeObserver().addOnScrollChangedListener(() -> onHoverMove(false));

        mFragmentLifecycleRegistry = new LifecycleRegistry(this);
        mViewModelStore = new ViewModelStore();
        mFragmentController = FragmentController.createController(this.new HostCallbacks());

        mFragmentController.attachHost(null);

        mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mFragmentController.dispatchCreate();

        mFragmentController.dispatchActivityCreated();
        mFragmentController.execPendingActions();

        mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        mFragmentController.dispatchStart();

        LOGGER.info(MARKER, "UI thread initialized in {}ms", (System.nanoTime() - startTime) / 1000000);

        // test stuff
        /*Paint paint = Paint.take();
        paint.setStrokeWidth(6);
        int c = (int) mElapsedTimeMillis / 300;
        c = Math.min(c, 8);
        float[] pts = new float[c * 2 + 2];
        pts[0] = 90;
        pts[1] = 30;
        for (int i = 0; i < c; i++) {
            pts[2 + i * 2] = Math.min((i + 2) * 60, mElapsedTimeMillis / 5) + 30;
            if ((i & 1) == 0) {
                if (mElapsedTimeMillis >= (i + 2) * 300) {
                    pts[3 + i * 2] = 90;
                } else {
                    pts[3 + i * 2] = 30 + (mElapsedTimeMillis % 300) / 5f;
                }
            } else {
                if (mElapsedTimeMillis >= (i + 2) * 300) {
                    pts[3 + i * 2] = 30;
                } else {
                    pts[3 + i * 2] = 90 - (mElapsedTimeMillis % 300) / 5f;
                }
            }
        }
        mCanvas.drawStripLines(pts, paint);

        paint.setRGBA(255, 180, 100, 255);
        mCanvas.drawCircle(90, 30, 6, paint);
        mCanvas.drawCircle(150, 90, 6, paint);
        mCanvas.drawCircle(210, 30, 6, paint);
        mCanvas.drawCircle(270, 90, 6, paint);
        mCanvas.drawCircle(330, 30, 6, paint);
        mCanvas.drawCircle(390, 90, 6, paint);
        mCanvas.drawCircle(450, 30, 6, paint);
        mCanvas.drawCircle(510, 90, 6, paint);
        mCanvas.drawCircle(570, 30, 6, paint);*/
    }

    @UiThread
    private void finish() {
        LOGGER.info(MARKER, "Quiting UI thread");

        mFragmentController.dispatchStop();
        mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);

        mFragmentController.dispatchDestroy();
        mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

        // must delay, some messages are not enqueued
        // currently it is a bit longer than a game tick
        mRoot.mHandler.postDelayed(mLooper::quitSafely, 60);
    }

    /**
     * From screen
     *
     * @param natural natural or synthetic
     * @see org.lwjgl.glfw.GLFWCursorPosCallbackI
     * @see net.minecraft.client.MouseHandler
     * @see MuiScreen
     */
    @MainThread
    void onHoverMove(boolean natural) {
        final long now = Core.timeNanos();
        float x = (float) (minecraft.mouseHandler.xpos() *
                mWindow.getWidth() / mWindow.getScreenWidth());
        float y = (float) (minecraft.mouseHandler.ypos() *
                mWindow.getHeight() / mWindow.getScreenHeight());
        MotionEvent event = MotionEvent.obtain(now, MotionEvent.ACTION_HOVER_MOVE,
                x, y, 0);
        mRoot.enqueueInputEvent(event);
        //mPendingRepostCursorEvent = false;
        if (natural && mButtonState > 0) {
            event = MotionEvent.obtain(now, MotionEvent.ACTION_MOVE, 0, x, y, 0, mButtonState, 0);
            mRoot.enqueueInputEvent(event);
        }
    }

    /**
     * @see org.lwjgl.glfw.GLFWMouseButtonCallbackI
     * @see net.minecraft.client.MouseHandler
     * @see net.minecraftforge.client.event.InputEvent
     */
    @SubscribeEvent
    void onPostMouseInput(@Nonnull InputEvent.MouseButton.Post event) {
        // We should ensure (overlay == null && screen != null)
        // and the screen must be a mui screen
        if (minecraft.getOverlay() == null && mScreen != null) {
            //ModernUI.LOGGER.info(MARKER, "Button: {} {} {}", event.getButton(), event.getAction(), event.getMods());
            final long now = Core.timeNanos();
            float x = (float) (minecraft.mouseHandler.xpos() *
                    mWindow.getWidth() / mWindow.getScreenWidth());
            float y = (float) (minecraft.mouseHandler.ypos() *
                    mWindow.getHeight() / mWindow.getScreenHeight());
            int buttonState = 0;
            for (int i = 0; i < 5; i++) {
                if (glfwGetMouseButton(mWindow.getWindow(), i) == GLFW_PRESS) {
                    buttonState |= 1 << i;
                }
            }
            mButtonState = buttonState;
            int action = event.getAction() == GLFW_PRESS ?
                    MotionEvent.ACTION_BUTTON_PRESS : MotionEvent.ACTION_BUTTON_RELEASE;
            int touchAction = event.getAction() == GLFW_PRESS ?
                    MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP;
            int actionButton = 1 << event.getButton();
            MotionEvent ev = MotionEvent.obtain(now, action, actionButton,
                    x, y, event.getModifiers(), buttonState, 0);
            mRoot.enqueueInputEvent(ev);
            if ((touchAction == MotionEvent.ACTION_DOWN && (buttonState ^ actionButton) == 0)
                    || (touchAction == MotionEvent.ACTION_UP && buttonState == 0)) {
                ev = MotionEvent.obtain(now, touchAction, actionButton,
                        x, y, event.getModifiers(), buttonState, 0);
                mRoot.enqueueInputEvent(ev);
                //LOGGER.info("Enqueue mouse event: {}", ev);
            }
        }
    }

    // Hook method, DO NOT CALL
    private void onScroll(double scrollX, double scrollY) {
        if (mScreen != null) {
            final long now = Core.timeNanos();
            final Window window = mWindow;
            final MouseHandler mouseHandler = minecraft.mouseHandler;
            float x = (float) (mouseHandler.xpos() *
                    window.getWidth() / window.getScreenWidth());
            float y = (float) (mouseHandler.ypos() *
                    window.getHeight() / window.getScreenHeight());
            MotionEvent event = MotionEvent.obtain(now, MotionEvent.ACTION_SCROLL,
                    x, y, 0);
            event.setAxisValue(MotionEvent.AXIS_HSCROLL, (float) scrollX);
            event.setAxisValue(MotionEvent.AXIS_VSCROLL, (float) scrollY);
            mRoot.enqueueInputEvent(event);
        }
    }

    @SubscribeEvent
    void onPostKeyInput(@Nonnull InputEvent.Key event) {
        if (mScreen != null) {
            int action = event.getAction() == GLFW_RELEASE ? KeyEvent.ACTION_UP : KeyEvent.ACTION_DOWN;
            KeyEvent keyEvent = KeyEvent.obtain(Core.timeNanos(), action, event.getKey(), 0,
                    event.getModifiers(), event.getScanCode(), 0);
            mRoot.enqueueInputEvent(keyEvent);
        }
        if (event.getAction() == GLFW_PRESS) {
            InputConstants.Key key = InputConstants.getKey(event.getKey(), event.getScanCode());
            if (OPEN_CENTER_KEY.isActiveAndMatches(key)) {
                start(new CenterFragment(), new UICallback());
                return;
            }
        }
        if (!Screen.hasControlDown() || !Screen.hasShiftDown() || !ModernUIFabric.isDeveloperMode()) {
            return;
        }
        if (event.getAction() == GLFW_PRESS) {
            switch (event.getKey()) {
                case GLFW_KEY_Y -> takeScreenshot();
                case GLFW_KEY_H -> start(new TestFragment(), new UICallback());
                case GLFW_KEY_J -> start(new TestPauseFragment(), new UICallback());
                case GLFW_KEY_U -> start(new TestListFragment(), new UICallback());
                case GLFW_KEY_N -> mDecor.postInvalidate();
                case GLFW_KEY_P -> dump();
                case GLFW_KEY_M -> changeRadialBlur();
                case GLFW_KEY_T -> {
                    String text = "\u09b9\u09cd\u09af\u09be\n\u09b2\u09cb" + ChatFormatting.RED + "\uD83E\uDD14" +
                            ChatFormatting.BOLD + "\uD83E\uDD14\uD83E\uDD14";
                    for (int i = 1; i <= 10; i++) {
                        float width = i * 5;
                        int index = ModernStringSplitter.breakText(text, width, Style.EMPTY, true);
                        LOGGER.info("Break forwards: width {} index:{}", width, index);
                        index = ModernStringSplitter.breakText(text, width, Style.EMPTY, false);
                        LOGGER.info("Break backwards: width {} index:{}", width, index);
                    }
                    LOGGER.info(TextLayoutEngine.getInstance().lookupVanillaNode(text));
                }
                case GLFW_KEY_G ->
                /*if (minecraft.screen == null && minecraft.isLocalServer() &&
                        minecraft.getSingleplayerServer() != null && !minecraft.getSingleplayerServer().isPublished()) {
                    start(new TestPauseUI());
                }*/
                /*minecraft.getLanguageManager().getLanguages().forEach(l ->
                        ModernUI.LOGGER.info(MARKER, "Locale {} RTL {}", l.getCode(), ULocale.forLocale(l
                        .getJavaLocale()).isRightToLeft()));*/
                        GlyphManager.getInstance().debug();
                case GLFW_KEY_V -> TextLayoutEngine.getInstance().dumpEmojiAtlas();
                case GLFW_KEY_F -> System.gc();
            }
        }
    }

    void takeScreenshot() {
        // take a screenshot from MSAA framebuffer
        GLTexture sampled = GLFramebuffer.resolve(mFramebuffer, GL_COLOR_ATTACHMENT0);
        NativeImage image = NativeImage.download(NativeImage.Format.RGBA, sampled, true);
        Util.ioPool().execute(() -> {
            try (image) {
                image.saveDialog(NativeImage.SaveFormat.PNG);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    void changeRadialBlur() {
        if (minecraft.gameRenderer.currentEffect() == null) {
            LOGGER.info(MARKER, "Load radial blur effect");
            minecraft.gameRenderer.loadEffect(new ResourceLocation("shaders/post/radial_blur.json"));
        } else {
            LOGGER.info(MARKER, "Stop post-processing effect");
            minecraft.gameRenderer.shutdownEffect();
        }
    }

    void dump() {
        StringBuilder builder = new StringBuilder();
        try (var w = new PrintWriter(new StringBuilderWriter(builder))) {
            dump(w);
        }
        String str = builder.toString();
        if (minecraft.level != null) {
            /*try {
                SEND_TO_CHAT.invoke(minecraft.gui.getChat(), ,
                        0xCBD366, minecraft.gui.getGuiTicks(), false);

            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }*/
            minecraft.gui.getChat().addMessage(Component.literal(str).withStyle(ChatFormatting.GRAY));
        }
        LOGGER.info(MARKER, str);
    }

    @SuppressWarnings("unchecked")
    private void dump(@Nonnull PrintWriter pw) {
        pw.println(">>> Modern UI dump data <<<");

        pw.print("Container Menu: ");
        LocalPlayer player = minecraft.player;
        AbstractContainerMenu menu = null;
        if (player != null) {
            menu = player.containerMenu;
        }
        if (menu != null) {
            pw.println(menu.getClass().getSimpleName());
            try {
                ResourceLocation name = ForgeRegistries.MENU_TYPES.getKey(menu.getType());
                pw.print("  Registry Name: ");
                pw.println(name);
            } catch (Exception ignored) {
            }
        } else {
            pw.println((Object) null);
        }

        Screen screen = minecraft.screen;
        if (screen != null) {
            pw.print("Screen: ");
            pw.println(screen.getClass());
        }

        if (mFragmentController != null) {
            mFragmentController.getFragmentManager().dump("", null, pw);
        }

        Map<ResourceLocation, AbstractTexture> textureMap = null;
        try {
            textureMap = ((AccessTextureManager)minecraft.getTextureManager()).getByPath();
        } catch (Exception ignored) {
        }
        if (textureMap != null && GLCore.getUnsupportedList().isEmpty()) {
            long gpuSize = 0;
            long cpuSize = 0;
            int dynamicTextures = 0;
            int textureAtlases = 0;
            int atlasSprites = 0;
            for (var texture : textureMap.values()) {
                try {
                    int tex = ((AccessAbstractTexture)texture).getTextureID();
                    if (glIsTexture(tex)) {
                        int internalFormat = glGetTextureLevelParameteri(tex, 0, GL_TEXTURE_INTERNAL_FORMAT);
                        long width = glGetTextureLevelParameteri(tex, 0, GL_TEXTURE_WIDTH);
                        long height = glGetTextureLevelParameteri(tex, 0, GL_TEXTURE_HEIGHT);
                        int maxLevel = glGetTextureParameteri(tex, GL_TEXTURE_MAX_LEVEL);
                        int bpp = switch (internalFormat) {
                            case GL_R8, GL_RED -> 1;
                            case GL_RG8, GL_RG -> 2;
                            case GL_RGB8, GL_RGBA8, GL_RGB, GL_RGBA -> 4;
                            default -> 0;
                        };
                        long size = width * height * bpp;
                        if (maxLevel > 0) {
                            size = ((size - (size >> ((maxLevel + 1) << 1))) << 2) / 3;
                        }
                        gpuSize += size;
                    }
                } catch (Exception ignored) {
                }

                if (texture instanceof DynamicTexture dynamicTexture) {
                    var image = dynamicTexture.getPixels();
                    try {
                        if (image != null && ((AccessNativeImage)((Object)image)).getPixels() != 0) {
                            cpuSize += (long) image.getWidth() * image.getHeight() * image.format().components();
                        }
                    } catch (Exception ignored) {
                    }
                    dynamicTextures++;
                }
                if (texture instanceof TextureAtlas textureAtlas) {
                    try {
                        Map<ResourceLocation, TextureAtlasSprite> textures =
                                ((AccessTextureAtlas)textureAtlas).getTexturesByName();
                        for (var sprite : textures.values()) {
                            for (var image : ((AccessTextureAtlasSprite)sprite).getMainImage()) {
                                if (image != null && ((AccessNativeImage)((Object)image)).getPixels() != 0) {
                                    cpuSize += (long) image.getWidth() * image.getHeight() * image.format().components();
                                }
                            }
                            atlasSprites++;
                        }
                    } catch (Exception ignored) {
                    }
                    textureAtlases++;
                }
            }
            pw.print("TextureManager: ");
            pw.print("Textures=" + textureMap.size());
            pw.print(", DynamicTextures=" + dynamicTextures);
            pw.print(", Atlases=" + textureAtlases);
            pw.print(", Sprites=" + atlasSprites);
            pw.print(", GPUMemory=" + TextUtils.binaryCompact(gpuSize) + " (" + gpuSize + " bytes)");
            pw.println(", CPUMemory=" + TextUtils.binaryCompact(cpuSize) + " (" + cpuSize + " bytes)");
        }

        ModernUIFabric.dispatchOnDebugDump(pw);
    }

    @MainThread
    boolean onCharTyped(char ch) {
        /*if (popup != null) {
            return popup.charTyped(codePoint, modifiers);
        }*/
        /*if (mKeyboard != null) {
            return mKeyboard.onCharTyped(codePoint, modifiers);
        }*/
        Message msg = Message.obtain(mRoot.mHandler, () -> {
            if (mDecor.findFocus() instanceof EditText text) {
                final Editable content = text.getText();
                int selStart = text.getSelectionStart();
                int selEnd = text.getSelectionEnd();
                if (selStart >= 0 && selEnd >= 0) {
                    Selection.setSelection(content, Math.max(selStart, selEnd));
                    content.replace(Math.min(selStart, selEnd), Math.max(selStart, selEnd), String.valueOf(ch));
                }
            }
        });
        msg.setAsynchronous(true);
        msg.sendToTarget();
        return true;//root.charTyped(codePoint, modifiers);
    }

    @RenderThread
    void render() {
        if (mCanvas == null) {
            if (mScreen != null) {
                String error = Language.getInstance().getOrDefault("error.modernui.gl_caps");
                int x = (mWindow.getGuiScaledWidth() - minecraft.font.width(error)) / 2;
                int y = (mWindow.getGuiScaledHeight() - 8) / 2;
                minecraft.font.draw(mEmptyPoseStack, error, x, y, 0xFFFF0000);
            }
            return;
        }
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.activeTexture(GL_TEXTURE0);
        RenderSystem.disableDepthTest();

        // blend alpha correctly, since the Minecraft.mainRenderTarget has no alpha (always 1)
        // and our framebuffer is always a transparent layer
        RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        final int oldVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        final int oldProgram = glGetInteger(GL_CURRENT_PROGRAM);

        // TODO need multiple canvas instances, tooltip shares this now, but different thread; remove Z transform
        mCanvas.setProjection(mProjectionMatrix.setOrthographic(
                mWindow.getWidth(), mWindow.getHeight(), 0, icyllis.modernui.core.Window.LAST_SYSTEM_WINDOW * 2 + 1,
                true));
        mRoot.flushDrawCommands(mCanvas, mFramebuffer, mWindow.getWidth(), mWindow.getHeight());

        glBindVertexArray(oldVertexArray);
        glUseProgram(oldProgram);

        RenderSystem.defaultBlendFunc();
        // force changing Blaze3D state
        RenderSystem.bindTexture(DEFAULT_TEXTURE);
    }

    @SubscribeEvent
    void onRenderGameOverlayLayer(@Nonnull RenderGuiOverlayEvent.Pre event) {
        /*switch (event.getType()) {
            case CROSSHAIRS:
                event.setCanceled(mScreen != null);
                break;
            case ALL:
                // hotfix 1.16 vanilla, using shader makes TEXTURE_2D disabled
                RenderSystem.enableTexture();
                break;
            *//*case HEALTH:
                if (TestHUD.sBars)
                    TestHUD.sInstance.drawBars(mFCanvas);
                break;*//*
        }*/
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
            if (mScreen != null) {
                event.setCanceled(true);
            }
            /*minecraft.font.draw(event.getMatrixStack(),
                    ChatFormatting.DARK_RED + "Fuck you " + ChatFormatting.UNDERLINE + "OK " + mElapsedTimeMillis +
                     " " + ChatFormatting.OBFUSCATED + "66" + ChatFormatting.RESET + " Fine", 20, 20, 0xFF0000);
            minecraft.font.draw(event.getMatrixStack(),
                    new TextComponent("Yes " + ChatFormatting.DARK_RED + "Fuck " + ChatFormatting.RESET + "That"),
                    20, 60, 0x00FFFF);*/
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    void onRenderTooltipH(@Nonnull RenderTooltipEvent.Pre event) {
        if (TooltipRenderer.sTooltip) {
            /*if (!(minecraft.font instanceof ModernFontRenderer)) {
                ModernUI.LOGGER.fatal(MARKER, "Failed to hook FontRenderer, tooltip disabled");
                TestHUD.sTooltip = false;
                return;
            }*/
            final Window window = mWindow;
            // screen coordinates to pixels for rendering
            final MouseHandler mouseHandler = minecraft.mouseHandler;
            // screen coordinates to pixels for rendering
            double cursorX = mouseHandler.xpos() *
                    (double) window.getGuiScaledWidth() / (double) window.getScreenWidth();
            double cursorY = mouseHandler.ypos() *
                    (double) window.getGuiScaledHeight() / (double) window.getScreenHeight();
            //if (event.getLines().isEmpty()) {
            mRoot.drawExtTooltipLocked(event, cursorX, cursorY); // need a lock
            /*} else {
                TooltipRenderer.drawTooltip(mCanvas, event.getLines(), event.getFontRenderer(), event.getStack(),
                        event.getMatrixStack(), event.getX(), event.getY(), (float) cursorX, (float) cursorY,
                        event.getMaxWidth(), event.getScreenWidth(), event.getScreenHeight(), window.getWidth(),
                        window.getHeight());
            }*/

            // our tooltip is translucent, need transparency sorting
            // we will cancel this event later, see below
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    void onRenderTooltipL(@Nonnull RenderTooltipEvent.Pre event) {
        if (TooltipRenderer.sTooltip) {
            event.setCanceled(true);
        }
    }

    private final Runnable mResizeRunnable = () -> mRoot.setFrame(mWindow.getWidth(), mWindow.getHeight());

    /**
     * Called when game window size changed, used to re-layout the window.
     */
    @MainThread
    void resize() {
        if (mRoot != null) {
            mRoot.mHandler.post(mResizeRunnable);
        }
    }

    @UiThread
    void updateLayoutDir(boolean forceRTL) {
        if (mDecor == null) {
            return;
        }
        boolean layoutRtl = forceRTL ||
                TextUtils.getLayoutDirectionFromLocale(ModernUI.getSelectedLocale()) == View.LAYOUT_DIRECTION_RTL;
        mDecor.setLayoutDirection(layoutRtl ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LOCALE);
        mDecor.requestLayout();
        TooltipRenderer.sLayoutRTL = layoutRtl;
    }

    @MainThread
    void removed() {
        MuiScreen screen = mScreen;
        if (screen == null) {
            return;
        }
        mRoot.mHandler.post(this::suppressLayoutTransition);
        mFragmentController.getFragmentManager().beginTransaction()
                .remove(screen.getFragment())
                .runOnCommit(() -> mFragmentContainerView.removeAllViews())
                .commit();
        mRoot.mHandler.post(this::restoreLayoutTransition);
        mScreen = null;
        glfwSetCursor(mWindow.getWindow(), MemoryUtil.NULL);
    }

    @SubscribeEvent
    void onRenderTick(@Nonnull TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            final long lastFrameTime = mFrameTimeNanos;
            mFrameTimeNanos = Core.timeNanos();
            final long deltaMillis = (mFrameTimeNanos - lastFrameTime) / 1000000;
            mElapsedTimeMillis += deltaMillis;
            // coordinates UI thread
            if (mRunning) {
                mRoot.mChoreographer.scheduleFrameAsync(mFrameTimeNanos);
                // update extension animations
                BlurHandler.INSTANCE.update(mElapsedTimeMillis);
                if (TooltipRenderer.sTooltip) {
                    TooltipRenderer.update(deltaMillis, mFrameTimeNanos / 1000000);
                }
            }
        } else {
            // main thread
            if (!minecraft.isRunning() && mRunning) {
                mRunning = false;
                mRoot.mHandler.post(this::finish);
                try {
                    // in case of GLFW is terminated too early
                    mUiThread.join(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (minecraft.isRunning() && mRunning && mScreen == null) {
                // Render the UI above everything
                render();
            }
        }
        /* else {
            // layout after updating animations and before drawing
            if (mLayoutRequested) {
                // fixed at 40Hz
                if (mElapsedTimeMillis - mLastLayoutTime >= 25) {
                    mLastLayoutTime = mElapsedTimeMillis;
                    doLayout();
                }
            }
        }*/
    }

    @UiThread
    class ViewRootImpl extends ViewRoot {

        private final Rect mGlobalRect = new Rect();

        ContextMenuBuilder mContextMenu;
        MenuHelper mContextMenuHelper;

        @Override
        protected Canvas beginRecording(int width, int height) {
            if (mCanvas != null) {
                mCanvas.reset(width, height);
            }
            return mCanvas;
        }

        @Override
        protected boolean dispatchTouchEvent(MotionEvent event) {
            if (mScreen != null && event.getAction() == MotionEvent.ACTION_DOWN) {
                View v = mDecor.findFocus();
                if (v instanceof EditText) {
                    v.getGlobalVisibleRect(mGlobalRect);
                    if (!mGlobalRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                        v.clearFocus();
                    }
                }
            }
            return super.dispatchTouchEvent(event);
        }

        @Override
        protected void onKeyEvent(KeyEvent event) {
            final MuiScreen screen = mScreen;
            if (screen != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                final boolean back;
                if (screen.getCallback() != null) {
                    back = screen.getCallback().isBackKey(event.getKeyCode(), event);
                } else if (screen instanceof MenuScreen) {
                    if (event.getKeyCode() == KeyEvent.KEY_ESCAPE) {
                        back = true;
                    } else {
                        InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
                        back = minecraft.options.keyInventory.isActiveAndMatches(key);
                    }
                } else {
                    back = event.getKeyCode() == KeyEvent.KEY_ESCAPE;
                }
                if (back) {
                    View v = mDecor.findFocus();
                    if (v instanceof EditText) {
                        if (event.getKeyCode() == KeyEvent.KEY_ESCAPE) {
                            mDecor.requestFocus();
                        }
                    } else {
                        mOnBackPressedDispatcher.onBackPressed();
                    }
                }
            }
        }

        @RenderThread
        private void flushDrawCommands(GLSurfaceCanvas canvas, GLFramebuffer framebuffer, int width, int height) {
            // wait UI thread, if slow
            synchronized (mRenderLock) {
                boolean blit = true;

                if (mRedrawn) {
                    mRedrawn = false;
                    glEnable(GL_STENCIL_TEST);
                    try {
                        blit = canvas.draw(framebuffer);
                    } catch (Throwable t) {
                        LOGGER.fatal(MARKER,
                                "Failed to invoke rendering callbacks, please report the issue to related mods", t);
                        dump();
                        throw t;
                    }
                    glDisable(GL_STENCIL_TEST);
                }

                final GLTexture layer = framebuffer.getAttachedTexture(GL_COLOR_ATTACHMENT0);
                if (blit && layer.getWidth() > 0) {
                    // draw MSAA off-screen target to Minecraft main target (not the default framebuffer)
                    RenderSystem.blendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                    glBindFramebuffer(GL_DRAW_FRAMEBUFFER, minecraft.getMainRenderTarget().frameBufferId);

                    // do alpha fade in
                    int alpha = (int) Math.min(0xff, mElapsedTimeMillis);
                    alpha = alpha << 8 | alpha;
                    alpha = alpha << 16 | alpha;
                    // premultiplied alpha
                    canvas.drawLayer(layer, width, height, alpha, true);
                    canvas.draw(null);
                }
            }
        }

        private void drawExtTooltipLocked(@Nonnull RenderTooltipEvent.Pre event, double cursorX, double cursorY) {
            if (mCanvas == null) {
                return;
            }
            synchronized (mRenderLock) {
                if (!mRedrawn) {
                    TooltipRenderer.drawTooltip(mCanvas, mWindow, event.getPoseStack(), event.getComponents(),
                            event.getX(), event.getY(), event.getFont(), event.getScreenWidth(),
                            event.getScreenHeight(), cursorX, cursorY, minecraft.getItemRenderer());
                }
            }
        }

        @Override
        public void playSoundEffect(int effectId) {
            /*if (effectId == SoundEffectConstants.CLICK) {
                minecraft.tell(() -> minecraft.getSoundManager().play(SimpleSoundInstance.forUI(MuiRegistries
                .BUTTON_CLICK_1, 1.0f)));
            }*/
        }

        @Override
        public boolean performHapticFeedback(int effectId, boolean always) {
            return false;
        }

        @MainThread
        protected void applyPointerIcon(int pointerType) {
            minecraft.tell(() -> glfwSetCursor(mWindow.getWindow(),
                    PointerIcon.getSystemIcon(pointerType).getHandle()));
        }

        @Override
        public boolean showContextMenuForChild(View originalView, float x, float y) {
            if (mContextMenuHelper != null) {
                mContextMenuHelper.dismiss();
                mContextMenuHelper = null;
            }

            if (mContextMenu == null) {
                mContextMenu = new ContextMenuBuilder();
                //mContextMenu.setCallback(callback);
            } else {
                mContextMenu.clearAll();
            }

            final MenuHelper helper;
            final boolean isPopup = !Float.isNaN(x) && !Float.isNaN(y);
            if (isPopup) {
                helper = mContextMenu.showPopup(originalView, x, y);
            } else {
                helper = mContextMenu.showPopup(originalView, 0, 0);
            }

            if (helper != null) {
                //helper.setPresenterCallback(callback);
            }

            mContextMenuHelper = helper;
            return helper != null;
        }
    }

    @UiThread
    class HostCallbacks extends FragmentHostCallback<Object> implements
            ViewModelStoreOwner,
            OnBackPressedDispatcherOwner {
        HostCallbacks() {
            super(new Handler(Looper.myLooper()));
            assert Core.isOnUiThread();
        }

        @Nullable
        @Override
        public Object onGetHost() {
            // intentionally null
            return null;
        }

        @Nullable
        @Override
        public View onFindViewById(int id) {
            return mDecor.findViewById(id);
        }

        @Nonnull
        @Override
        public ViewModelStore getViewModelStore() {
            return mViewModelStore;
        }

        @Nonnull
        @Override
        public OnBackPressedDispatcher getOnBackPressedDispatcher() {
            return mOnBackPressedDispatcher;
        }

        @Nonnull
        @Override
        public Lifecycle getLifecycle() {
            return mFragmentLifecycleRegistry;
        }
    }

    //boolean mPendingRepostCursorEvent = false;

    // main fragment of a UI
    //private Fragment fragment;

    // main UI view that created from main fragment
    //private View view;

    //private IModule popup;

    //boolean mLayoutRequested = false;
    //private long mLastLayoutTime = 0;

    // elapsed ticks from a gui open, update every tick, 20 = 1 second
    //private int mTicks = 0;

    // registered menu screens
    //private final Map<ContainerType<?>, Function<? extends Container, ApplicationUI>> mScreenRegistry = new
    // HashMap<>();

    // the most child hovered view, render at the top of other hovered ancestor views
    /*@Nullable
    private View mHovered;*/

    // focused view
    /*@Nullable
    private View mDragging;
    @Nullable
    private View mKeyboard;*/

    /*public int getScreenWidth() {
        return mWidth;
    }

    public int getScreenHeight() {
        return mHeight;
    }

    public double getCursorX() {
        return mCursorX;
    }

    public double getCursorY() {
        return mCursorY;
    }

    public double getViewMouseX(@Nonnull View view) {
        ViewParent parent = view.getParent();
        double mouseX = mCursorX;

        while (parent != null) {
            mouseX += parent.getScrollX();
            parent = parent.getParent();
        }

        return mouseX;
    }

    public double getViewMouseY(@Nonnull View view) {
        ViewParent parent = view.getParent();
        double mouseY = mCursorY;

        while (parent != null) {
            mouseY += parent.getScrollY();
            parent = parent.getParent();
        }

        return mouseY;
    }*/

    /*void setHovered(@Nullable View view) {
        mHovered = view;
    }

    @Nullable
    public View getHovered() {
        return mHovered;
    }

    public void setDragging(@Nullable View view) {
        if (mDragging != view) {
            if (mDragging != null) {
                mDragging.onStopDragging();
            }
            mDragging = view;
            if (mDragging != null) {
                mDragging.onStartDragging();
            }
        }
    }

    @Nullable
    public View getDragging() {
        return mDragging;
    }

    public void setKeyboard(@Nullable View view) {
        if (mKeyboard != view) {
            minecraft.keyboardHandler.setSendRepeatsToGui(view != null);
            if (mKeyboard != null) {
                mKeyboard.onStopKeyboard();
            }
            mKeyboard = view;
            if (mKeyboard != null) {
                mKeyboard.onStartKeyboard();
            }
        }
    }

    @Nullable
    public View getKeyboard() {
        return mKeyboard;
    }*/

    /*@Nonnull
    private <T extends Container> ScreenManager.IScreenFactory<T, MuiMenuScreen<T>> getFactory(
            @Nonnull Function<T, Fragment> factory) {
        return (container, inventory, title) -> {
            this.fragment = factory.apply(container);
            return new MuiMenuScreen<>(container, inventory, title, this);
        };
    }*/

    /*
     * Get elapsed ticks from a gui open, update every tick, 20 = 1 second
     *
     * @return elapsed ticks
     */
    /*public int getElapsedTicks() {
        return mTicks;
    }*/

    /*@Deprecated
    public void openPopup(IModule popup, boolean refresh) {
        throw new UnsupportedOperationException();
        *//*if (root == null) {
            ModernUI.LOGGER.fatal(MARKER, "#openPopup() shouldn't be called when there's NO gui open");
            return;
        }*//*
     *//*if (this.popup != null) {
            ModernUI.LOGGER.warn(MARKER, "#openPopup() shouldn't be called when there's already a popup, the previous
             one has been overwritten");
        }
        if (refresh) {
            this.screenMouseMove(-1, -1);
        }
        this.popup = popup;
        this.popup.resize(width, height);*//*
    }*/

    /*@Deprecated
    public void closePopup() {
        throw new UnsupportedOperationException();
        *//*if (popup != null) {
            popup = null;
        }*//*
    }*/

    /*boolean screenKeyDown(int keyCode, int scanCode, int modifiers) {
     *//*if (popup != null) {
            return popup.keyPressed(keyCode, scanCode, modifiers);
        }*//*
        ModernUI.LOGGER.debug(MARKER, "KeyDown{keyCode:{}, scanCode:{}, mods:{}}", keyCode, scanCode, modifiers);
        *//*if (mKeyboard != null) {
            return mKeyboard.onKeyPressed(keyCode, scanCode, modifiers);
        }*//*
        return false;
    }*/

    /*boolean screenKeyUp(int keyCode, int scanCode, int modifiers) {
     *//*if (popup != null) {
            return popup.keyReleased(keyCode, scanCode, modifiers);
        }*//*
     *//*if (mKeyboard != null) {
            return mKeyboard.onKeyReleased(keyCode, scanCode, modifiers);
        }*//*
        return false;//root.keyReleased(keyCode, scanCode, modifiers);
    }*/

    /*boolean sChangeKeyboard(boolean searchNext) {
        return false;
    }

    boolean onBackPressed() {
        *//*if (popup != null) {
            closePopup();
            return true;
        }*//*
        return false;//root.onBack();
    }*/

    /*
     * Get current open screen differently from Minecraft's,
     * which will only return Modern UI's screen or null
     *
     * @return open modern screen
     * @see Minecraft#currentScreen
     */
    /*@Nullable
    public Screen getModernScreen() {
        return mMuiScreen;
    }*/

    /*public boolean hasOpenGUI() {
        return mScreen != null;
    }*/

    /*public void repostCursorEvent() {
        mPendingRepostCursorEvent = true;
    }*/
    
    /*@Deprecated
    boolean screenMouseDown(double mouseX, double mouseY, int mouseButton) {
        setMousePos(mouseX, mouseY);
        MotionEvent event = null;
        event.button = mouseButton;
        //List<ViewRootImpl> windows = this.windows;
        boolean handled = false;
        if (mouseButton == GLFW_MOUSE_BUTTON_LEFT && lastLmTick >= 0 && ticks - lastLmTick < 6) {
            //event.action = MotionEvent.ACTION_DOUBLE_CLICK;
            *//*for (int i = windows.size() - 1; i >= 0; i--) {
                if (windows.get(i).onMouseEvent(event)) {
                    return true;
                }
            }*//*
            if (lastLmView != null && lastLmView.isMouseHovered() && lastLmView.onGenericMotionEvent(event)) {
                handled = true;
            }
        }
        lastLmView = null;
        if (handled) {
            return true;
        }
        //event.action = MotionEvent.ACTION_PRESS;
        return mAppWindow.onMouseEvent(event);
        *//*for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).onMouseEvent(event)) {
                return true;
            }
        }*//*
     *//*if (popup != null) {
            return popup.mouseClicked(mouseX, mouseY, mouseButton);
        }*//*
     *//*if (mHovered != null) {
            IViewParent parent;
            View view;
            double viewMX = getViewMouseX(mHovered);
            double viewMY = getViewMouseY(mHovered);
            if (mouseButton == 0) {
                int delta = ticks - lastDClickTick;
                if (delta < 10) {
                    lastDClickTick = Integer.MIN_VALUE;
                    if (mHovered.onMouseDoubleClicked(viewMX, viewMY)) {
                        return true;
                    }
                    parent = mHovered.getParent();
                    double viewMX2 = viewMX;
                    double viewMY2 = viewMY;
                    while (parent instanceof View) {
                        view = (View) parent;
                        viewMX2 -= parent.getScrollX();
                        viewMY2 -= parent.getScrollY();
                        if (view.onMouseDoubleClicked(viewMX2, viewMY2)) {
                            return true;
                        }
                        parent = parent.getParent();
                    }
                } else {
                    lastDClickTick = ticks;
                }
            }
            *//**//*if (mHovered.mouseClicked(viewMX, viewMY, mouseButton)) {
                return true;
            }*//**//*
            parent = mHovered.getParent();
            while (parent instanceof View) {
                view = (View) parent;
                viewMX -= parent.getScrollX();
                viewMY -= parent.getScrollY();
                *//**//*if (view.mouseClicked(viewMX, viewMY, mouseButton)) {
                    return true;
                }*//**//*
                parent = parent.getParent();
            }
        }*//*
    }

    @Deprecated
    boolean screenMouseUp(double mouseX, double mouseY, int mouseButton) {
        setMousePos(mouseX, mouseY);
        MotionEvent event = motionEvent;
        //event.action = MotionEvent.ACTION_RELEASE;
        event.button = mouseButton;
        boolean dCheck = false;
        if (mouseButton == GLFW_MOUSE_BUTTON_LEFT && lastLmTick < 0) {
            dCheck = event.pressMap.get(mouseButton) != null;
        } else {
            lastLmTick = Integer.MIN_VALUE;
        }
        //List<ViewRootImpl> windows = this.windows;
        boolean handled = false;
        *//*for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).onMouseEvent(event)) {
                handled = true;
                break;
            }
        }*//*
        if (mAppWindow.onMouseEvent(event)) {
            handled = true;
        }
        if (dCheck && event.clicked != null) {
            lastLmTick = ticks;
        } else {
            lastLmTick = Integer.MIN_VALUE;
        }
        lastLmView = event.clicked;
        event.clicked = null;
        if (handled) {
            return true;
        }
        *//*if (popup != null) {
            return popup.mouseReleased(mouseX, mouseY, mouseButton);
        }*//*
        if (mDragging != null) {
            setDragging(null);
            return true;
        }
        *//*if (mHovered != null) {
            double viewMX = getViewMouseX(mHovered);
            double viewMY = getViewMouseY(mHovered);
            if (mHovered.onMouseReleased(viewMX, viewMY, mouseButton)) {
                return true;
            }
            IViewParent parent = mHovered.getParent();
            View view;
            while (parent instanceof View) {
                view = (View) parent;
                viewMX -= parent.getScrollX();
                viewMY -= parent.getScrollY();
                if (view.onMouseReleased(viewMX, viewMY, mouseButton)) {
                    return true;
                }
                parent = parent.getParent();
            }
        }*//*
        return false;//root.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Deprecated
    boolean screenMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        setMousePos(mouseX, mouseY);
        *//*if (popup != null) {
            return popup.mouseDragged(mouseX, mouseY, deltaX, deltaY);
        }*//*
        if (mDragging != null) {
            return mDragging.onMouseDragged(getViewMouseX(mDragging), getViewMouseY(mDragging), deltaX, deltaY);
        }
        return false;
    }

    @Deprecated
    boolean screenMouseScroll(double mouseX, double mouseY, double amount) {
        setMousePos(mouseX, mouseY);
        MotionEvent event = motionEvent;
        //event.action = MotionEvent.ACTION_SCROLL;
        event.scrollDelta = amount;
        *//*List<ViewRootImpl> windows = this.windows;
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).onMouseEvent(event)) {
                return true;
            }
        }*//*
        return mAppWindow.onMouseEvent(event);
        *//*if (popup != null) {
            return popup.mouseScrolled(mouseX, mouseY, amount);
        }*//*
     *//*if (mHovered != null) {
            double viewMX = getViewMouseX(mHovered);
            double viewMY = getViewMouseY(mHovered);
            if (mHovered.onMouseScrolled(viewMX, getViewMouseY(mHovered), amount)) {
                return true;
            }
            IViewParent parent = mHovered.getParent();
            View view;
            while (parent != null) {
                view = (View) parent;
                viewMX -= parent.getScrollX();
                viewMY -= parent.getScrollY();
                if (view.onMouseScrolled(mouseX, mouseY, amount)) {
                    return true;
                }
                parent = parent.getParent();
            }
        }*//*
    }*/

    /*private void setMousePos(double mouseX, double mouseY) {
     *//*this.mouseX = mouseEvent.x = mouseX;
        this.mouseY = mouseEvent.y = mouseY;*//*
    }

    @Deprecated
    void screenMouseMove(double mouseX, double mouseY) {
        setMousePos(mouseX, mouseY);
        MotionEvent event = null;
        //event.action = MotionEvent.ACTION_MOVE;
        *//*List<ViewRootImpl> windows = this.windows;
        boolean anyHovered = false;
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (!anyHovered && windows.get(i).onMouseEvent(event)) {
                anyHovered = true;
            } else {
                windows.get(i).ensureMouseHoverExit();
            }
        }*//*
        mAppWindow.onMouseEvent(event);
        cursorRefreshRequested = false;
    }*/

    /*private void mouseMoved() {
        if (view != null && !view.updateMouseHover(mouseX, mouseY)) {
            setHovered(null);
        }
    }*/
}
