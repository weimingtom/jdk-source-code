/*
 * @(#)CoverageTables.h	1.9 05/05/11
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved
 *
 */

#ifndef __COVERAGETABLES_H
#define __COVERAGETABLES_H

#include "LETypes.h"
#include "OpenTypeTables.h"

struct CoverageTable
{
    le_uint16 coverageFormat;

    le_int32 getGlyphCoverage(LEGlyphID glyphID) const;
};

struct CoverageFormat1Table : CoverageTable
{
    le_uint16  glyphCount;
    TTGlyphID glyphArray[ANY_NUMBER];

    le_int32 getGlyphCoverage(LEGlyphID glyphID) const;
};

struct CoverageFormat2Table : CoverageTable
{
    le_uint16        rangeCount;
    GlyphRangeRecord rangeRecordArray[ANY_NUMBER];

    le_int32 getGlyphCoverage(LEGlyphID glyphID) const;
};


#endif
