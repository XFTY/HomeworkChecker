package com.xfty.homeworkchecker.service.ui.motion;

import javafx.animation.Interpolator;

/**
 * MotionEasing —— 统一的动画缓动曲线。
 * <p>
 * 位移、缩放、尺寸等几何动画默认使用类 iOS 弹簧曲线，让运动带有轻微的
 * 「Q 弹」收束感；卡片删除等明确不需要弹性的场景使用标准非线性减速曲线。
 * 透明度渐隐/渐显可选用本类的无过冲弹簧曲线，使淡入淡出与几何动画节奏一致；
 * 颜色、模糊等其他非几何动画仍保持原有观感。
 * </p>
 */
public final class MotionEasing {

    /**
     * 出现、放大、归位使用的阻尼弹簧。
     * <p>
     * 曲线采用归一化二阶欠阻尼弹簧阶跃响应，从静止开始加速，约在动画中段
     * 产生一次 5% 左右的轻微过冲，随后小幅回落并稳定到目标值，
     * 形成 iOS 风格的弹性收束。
     * </p>
     */
    public static final Interpolator SPRING = new DampedSpringInterpolator(6.5, 7.0);

    /**
     * 收起、关闭、缩小使用的无过冲收束曲线。
     * <p>
     * 尺寸类属性不能接受负值，因此退场动画使用快速减速、不回弹的曲线，
     * 避免高度或缩放出现反向越界。
     * </p>
     */
    public static final Interpolator SPRING_EXIT = Interpolator.SPLINE(0.22, 1.0, 0.36, 1.0);

    /**
     * 卡片删除、空间收回使用的非线性减速曲线。
     * <p>
     * 采用标准三次贝塞尔 {@code cubic-bezier(0.33, 1, 0.68, 1)}：起步较快、
     * 末段平滑减速，且不会越过目标值。它不是弹簧曲线，不会产生回弹。
     * </p>
     */
    public static final Interpolator EASE_OUT_CUBIC = Interpolator.SPLINE(0.33, 1.0, 0.68, 1.0);

    /**
     * 透明度渐隐/渐显使用的无过冲弹簧曲线。
     * <p>
     * 与 {@link #SPRING} 一样从静止开始加速、随后逐渐减速收束，但采用临界阻尼，
     * 不会越过目标值。适合 0~1 范围的 opacity，既能避免线性渐变的机械感，
     * 也不会像欠阻尼曲线那样出现过冲后透明度被截断的停顿。
     * </p>
     */
    public static final Interpolator SPRING_FADE = new CriticalSpringInterpolator(5.5);

    /** 工具类不允许实例化。 */
    private MotionEasing() {
    }

    /**
     * 归一化二阶欠阻尼弹簧插值器。
     * <p>
     * 原始公式为：
     * {@code 1 - e^(-damping*t) * (cos(frequency*t) + damping/frequency * sin(frequency*t))}。
     * 该阶跃响应在 t=0 时速度为 0，不会一启动就冲到目标附近；为保证动画结束时
     * 精确到达 1，曲线整体除以终点值再做归一化。
     * </p>
     */
    private static final class DampedSpringInterpolator extends Interpolator {

        /** 阻尼系数（ζωn），越大回弹越弱、收束越快。 */
        private final double damping;
        /** 有阻尼振荡角频率（ωd），越大过冲出现得越早、振荡越密。 */
        private final double frequency;
        /** 终点原始值，用于把曲线归一化到 0~1 端点。 */
        private final double endValue;

        /**
         * 构造阻尼弹簧插值器。
         *
         * @param damping   阻尼系数
         * @param frequency 有阻尼振荡角频率
         */
        private DampedSpringInterpolator(double damping, double frequency) {
            this.damping = damping;
            this.frequency = frequency;
            // 预先计算 t=1 时的原始值，避免每次插值重复计算
            this.endValue = rawCurve(1.0);
        }

        /**
         * 计算归一化后的弹簧进度。
         *
         * @param t 原始时间进度（0~1）
         * @return 带轻微过冲的插值进度
         */
        @Override
        protected double curve(double t) {
            // 端点直接返回精确值，避免浮点误差导致动画结束位置偏移
            if (t <= 0.0) {
                return 0.0;
            }
            if (t >= 1.0) {
                return 1.0;
            }
            return rawCurve(t) / endValue;
        }

        /**
         * 计算未归一化的二阶欠阻尼弹簧阶跃响应。
         *
         * @param t 时间进度
         * @return 原始响应值
         */
        private double rawCurve(double t) {
            // 初始速度为 0：缺少 sin 项会让动画启动瞬间过快
            return 1.0 - Math.exp(-damping * t)
                * (Math.cos(frequency * t) + damping / frequency * Math.sin(frequency * t));
        }
    }

    /**
     * 归一化二阶临界阻尼弹簧插值器。
     * <p>
     * 原始公式为 {@code 1 - e^(-damping*t) * (1 + damping*t)}。
     * 该曲线单调上升且没有过冲，初始速度同样为 0，收束节奏与欠阻尼弹簧一致，
     * 但不会让 opacity 越过 0 或 1。
     * </p>
     */
    private static final class CriticalSpringInterpolator extends Interpolator {

        /** 阻尼系数，越大收敛越快。 */
        private final double damping;
        /** 终点原始值，用于把曲线归一化到 0~1 端点。 */
        private final double endValue;

        /**
         * 构造临界阻尼弹簧插值器。
         *
         * @param damping 阻尼系数
         */
        private CriticalSpringInterpolator(double damping) {
            this.damping = damping;
            // 预先计算 t=1 时的原始值，保证动画结束时精确到达 1
            this.endValue = rawCurve(1.0);
        }

        /**
         * 计算归一化后的临界阻尼弹簧进度。
         *
         * @param t 原始时间进度（0~1）
         * @return 无过冲的插值进度
         */
        @Override
        protected double curve(double t) {
            // 端点直接返回精确值，避免浮点误差导致透明度末端残留
            if (t <= 0.0) {
                return 0.0;
            }
            if (t >= 1.0) {
                return 1.0;
            }
            return rawCurve(t) / endValue;
        }

        /**
         * 计算未归一化的临界阻尼弹簧阶跃响应。
         *
         * @param t 时间进度
         * @return 原始响应值
         */
        private double rawCurve(double t) {
            return 1.0 - Math.exp(-damping * t) * (1.0 + damping * t);
        }
    }
}
