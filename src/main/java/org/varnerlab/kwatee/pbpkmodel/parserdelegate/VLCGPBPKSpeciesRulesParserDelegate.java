package org.varnerlab.kwatee.pbpkmodel.parserdelegate;

import org.varnerlab.kwatee.pbpkmodel.model.VLCGPBPKCompartmentConnectionModel;
import org.varnerlab.kwatee.pbpkmodel.model.VLCGPBPKModelComponent;
import org.varnerlab.kwatee.pbpkmodel.model.VLCGPBPKSpeciesModel;

import java.util.StringTokenizer;

/**
 * Copyright (c) 2015 Varnerlab,
 * School of Chemical Engineering,
 * Purdue University, West Lafayette IN 46077 USA.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * <p>
 * Created by jeffreyvarner on 10/30/15.
 */
public class VLCGPBPKSpeciesRulesParserDelegate implements VLCGParserHandlerDelegate {

    // instance variables -
    private VLCGPBPKModelComponent _model = null;

    @Override
    public Object parseLine(String line) throws Exception {

        // Build a species model with the rule type -
        _model = new VLCGPBPKSpeciesModel();

        // cache line -
        _model.setModelComponent(VLCGPBPKCompartmentConnectionModel.RAW_RECORD,line);

        // Parse this line -
        StringTokenizer stringTokenizer = new StringTokenizer(line,",");
        int counter = 1;
        while (stringTokenizer.hasMoreTokens()) {

            // Get the token -
            String token = (String) stringTokenizer.nextToken();

            // record -
            // species[1],type[2];

            if (counter == 1){
                String strTmp = ((String) token).replace("-", "_");
                _model.setModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL,strTmp);
            }
            else if (counter == 2){

                // remove the ;
                String strTmp = token.substring(0, token.length() - 1);
                _model.setModelComponent(VLCGPBPKSpeciesModel.SPECIES_RULE_TYPE,strTmp);
            }
            else {
                throw new Exception("The parseLine(...) method of "+this.getClass().toString() + " does not support more than "+(counter - 1)+" tokens. Check: "+line);
            }

            // update -
            counter++;
        }

        return _model;
    }
}
