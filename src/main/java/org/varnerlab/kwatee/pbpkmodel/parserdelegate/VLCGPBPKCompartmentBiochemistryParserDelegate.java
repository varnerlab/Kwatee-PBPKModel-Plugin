package org.varnerlab.kwatee.pbpkmodel.parserdelegate;

import org.varnerlab.kwatee.pbpkmodel.model.VLCGPBPKBiochemistryReactionModel;
import org.varnerlab.kwatee.pbpkmodel.model.VLCGPBPKCompartmentConnectionModel;
import org.varnerlab.kwatee.pbpkmodel.model.VLCGPBPKModelComponent;

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
public class VLCGPBPKCompartmentBiochemistryParserDelegate implements VLCGParserHandlerDelegate {

    // instance variables -
    private VLCGPBPKModelComponent _model = null;


    @Override
    public Object parseLine(String line) throws Exception {


        // ok, create a model instance -
        _model = new VLCGPBPKBiochemistryReactionModel();

        // cache line -
        _model.setModelComponent(VLCGPBPKBiochemistryReactionModel.FORMATTED_RAW_RECORD,_formatBiochemistryString(line));
        _model.setModelComponent(VLCGPBPKBiochemistryReactionModel.RAW_RECORD,line);

        // Parse this line -
        StringTokenizer stringTokenizer = new StringTokenizer(line,",");
        int counter = 1;
        while (stringTokenizer.hasMoreTokens()) {

            // Get the token -
            String token = (String) stringTokenizer.nextToken();

            // record is:
            // name[1],compartment[2],reactant_string[3],product_string[4],reverse[5],forward[6];
            if (counter == 1){
                _model.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_NAME,token);
            }
            else if (counter == 2){
                String strTmp = ((String)token).replace("-", "_");
                _model.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_COMPARTMENT_SYMBOL,strTmp);
            }
            else if (counter == 3){
                String strTmp = ((String)token).replace("-", "_");
                _model.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_REACTANTS,strTmp);
            }
            else if (counter == 4){
                String strTmp = ((String)token).replace("-", "_");
                _model.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_PRODUCTS,strTmp);
            }
            else if (counter == 5){
                _model.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_REVERSE,token);
            }
            else if (counter == 6){
                // remove the ;
                String strTmp = token.substring(0,token.length() - 1);
                _model.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_FORWARD,strTmp);
            }
            else {
                throw new Exception("The parseLine method of "+this.getClass().toString() + " does not support > six tokens. Incorrect format for line: "+line);
            }


            // update the counter -
            counter++;
        }

        // return the model -
        return _model;
    }


    private String _formatBiochemistryString(String line) throws Exception {

        // method variables -
        int counter = 1;
        StringBuffer buffer = new StringBuffer();

        // split around the ','
        StringTokenizer stringTokenizer = new StringTokenizer(line,",");
        while (stringTokenizer.hasMoreElements()){

            // Get the token -
            String token = (String)stringTokenizer.nextToken();

            if (counter == 1){
                buffer.append(token);
                buffer.append(": ");
            }
            else if (counter == 2){
                buffer.append("compartment: ");
                buffer.append(token);
                buffer.append(" ");
            }
            else if (counter == 3){
                buffer.append(token);
                buffer.append(" = ");
            }
            else if (counter == 4){
                buffer.append(token);
            }

            // update the counter -
            counter++;
        }

        // return the buffer -
        return buffer.toString();
    }
}
