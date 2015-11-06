package org.varnerlab.kwatee.pbpkmodel.parserdelegate;

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
public class VLCGPBPKCompartmentConnectionParserDelegate implements VLCGParserHandlerDelegate {

    // instance variables -
    private VLCGPBPKModelComponent _model = null;

    @Override
    public Object parseLine(String line) throws Exception {

        // ok, create a model instance -
        _model = new VLCGPBPKCompartmentConnectionModel();

        // cache line -
        _model.setModelComponent(VLCGPBPKCompartmentConnectionModel.FORMATTED_RAW_RECORD,_formatConnectionString(line));
        _model.setModelComponent(VLCGPBPKCompartmentConnectionModel.RAW_RECORD,line);

        // Parse this line -
        StringTokenizer stringTokenizer = new StringTokenizer(line,",");
        int counter = 1;
        while (stringTokenizer.hasMoreTokens()) {

            // Get the token -
            String token = (String) stringTokenizer.nextToken();

            if (counter == 1) {
                _model.setModelComponent(VLCGPBPKCompartmentConnectionModel.COMPARTMENT_CONNECTION_NAME, token);
            } else if (counter == 2) {

                String strTmp = ((String) token).replace("-", "_");
                _model.setModelComponent(VLCGPBPKCompartmentConnectionModel.COMPARTMENT_CONNECTION_SOURCE, strTmp);
            } else if (counter == 3) {

                String strTmp = ((String) token).replace("-", "_");
                _model.setModelComponent(VLCGPBPKCompartmentConnectionModel.COMPARTMENT_CONNECTION_TARGET, strTmp);

            } else if (counter == 4){

                _model.setModelComponent(VLCGPBPKCompartmentConnectionModel.COMPARTMENT_CONNECTION_REVERSE, token);
            }
            else if (counter == 5) {
                // remove the ;
                String strTmp = token.substring(0, token.length() - 1);
                _model.setModelComponent(VLCGPBPKCompartmentConnectionModel.COMPARTMENT_CONNECTION_FORWARD, strTmp);
            } else {
                throw new Exception("The parseLine(...) method of "+this.getClass().toString() + " does not support more than give tokens. Check: "+line);
            }

            // update the counter -
            counter++;
        }

        return _model;
    }

    private String _formatConnectionString(String line) throws Exception {

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
                buffer.append(token);
                buffer.append(" => ");
            }
            else if (counter == 3){
                buffer.append(token);
            }

            // update the counter -
            counter++;
        }

        // return the buffer -
        return buffer.toString();
    }
}
