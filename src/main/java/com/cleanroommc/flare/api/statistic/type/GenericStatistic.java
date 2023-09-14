/*
 * This file is part of spark, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.cleanroommc.flare.api.statistic.type;

import com.cleanroommc.flare.api.statistic.StatisticWindow;

import javax.annotation.Nonnull;

/**
 * A {@link Statistic} with a generic (non-primitive) type.
 *
 * @param <T> the generic value type
 * @param <W> the window type
 */
public interface GenericStatistic<T, W extends Enum<W> & StatisticWindow> extends Statistic<W> {

    /**
     * Polls the current value of the statistic in the given window.
     *
     * @param window the window
     * @return the value
     */
    T poll(@Nonnull W window);

    /**
     * Polls the current values of the statistic in all windows.
     *
     * @return the values
     */
    T[] poll();

}
