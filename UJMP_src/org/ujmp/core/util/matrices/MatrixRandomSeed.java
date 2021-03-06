/*
 * Copyright (C) 2008-2010 by Holger Arndt
 *
 * This file is part of the Universal Java Matrix Package (UJMP).
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * UJMP is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * UJMP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with UJMP; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package org.ujmp.core.util.matrices;

import org.ujmp.core.longmatrix.stub.AbstractDenseLongMatrix2D;
import org.ujmp.core.util.MathUtil;

public class MatrixRandomSeed extends AbstractDenseLongMatrix2D {
	private static final long serialVersionUID = 1950244958868637395L;

	public MatrixRandomSeed() {
		setLabel("Random Seed");
	}

	public long[] getSize() {
		return new long[] { 1, 1 };
	}

	public long getLong(long row, long column) {
		return MathUtil.getSeed();
	}

	public void setLong(long value, long row, long column) {
		MathUtil.setSeed(value);
	}

	public long getLong(int row, int column) {
		return MathUtil.getSeed();
	}

	public void setLong(long value, int row, int column) {
		MathUtil.setSeed(value);
	}

}
