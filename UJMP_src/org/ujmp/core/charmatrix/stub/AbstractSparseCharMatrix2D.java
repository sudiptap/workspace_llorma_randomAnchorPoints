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

package org.ujmp.core.charmatrix.stub;

import org.ujmp.core.charmatrix.SparseCharMatrix2D;
import org.ujmp.core.util.CoordinateIterator2D;

public abstract class AbstractSparseCharMatrix2D extends AbstractSparseCharMatrix implements
		SparseCharMatrix2D {

	private static final long serialVersionUID = -3805321813335029419L;

	public final Iterable<long[]> allCoordinates() {
		return new CoordinateIterator2D(getSize());
	}

	public final char getChar(long... coordinates) {
		return getChar(coordinates[ROW], coordinates[COLUMN]);
	}

	public final void setChar(char value, long... coordinates) {
		setChar(value, coordinates[ROW], coordinates[COLUMN]);
	}

	
	public final Character getObject(long row, long column) {
		return getChar(row, column);
	}

	
	public final Character getObject(int row, int column) {
		return getChar(row, column);
	}

	
	public final void setObject(Character value, long row, long column) {
		setChar(value, row, column);
	}

	
	public final void setObject(Character value, int row, int column) {
		setChar(value, row, column);
	}
}
