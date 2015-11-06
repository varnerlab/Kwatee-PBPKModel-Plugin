package org.varnerlab.kwatee.pbpkmodel;

import org.varnerlab.kwatee.foundation.VLCGOutputHandler;
import org.varnerlab.kwatee.foundation.VLCGTransformationPropertyTree;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

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
public class VLCGWriteJuliaPBPKModel implements VLCGOutputHandler {

    // instance variables -
    private VLCGTransformationPropertyTree _transformation_properties_tree = null;
    private VLCGJuliaPBPKModelDelegate _delegate_object = new VLCGJuliaPBPKModelDelegate();


    @Override
    public void writeResource(Object object) throws Exception {

        // Grab the model tree -
        VLCGPBPKModelTreeWrapper model_wrapper = (VLCGPBPKModelTreeWrapper)object;

        // create a method array -
        ArrayList<String> method_name_array_list = new ArrayList();
        method_name_array_list.add("buildDriverFunctionBuffer");
        method_name_array_list.add("buildDataDictionaryFunctionBuffer");
        method_name_array_list.add("buildKineticsFunctionBuffer");
        method_name_array_list.add("buildControlFunctionBuffer");
        method_name_array_list.add("buildBalanceFunctionBuffer");

        // Create path dictionary -
        HashMap<String,String> path_map = new HashMap<String,String>();
        path_map.put("buildDriverFunctionBuffer",_transformation_properties_tree.lookupKwateeDriverFunctionFilePath());
        path_map.put("buildDataDictionaryFunctionBuffer",_transformation_properties_tree.lookupKwateeDataDictionaryFilePath());
        path_map.put("buildKineticsFunctionBuffer",_transformation_properties_tree.lookupKwateeKineticsFunctionFilePath());
        path_map.put("buildControlFunctionBuffer",_transformation_properties_tree.lookupKwateeControlFunctionFilePath());
        path_map.put("buildBalanceFunctionBuffer",_transformation_properties_tree.lookupKwateeBalanceFunctionFilePath());

        // execution loop -
        for (String method_name : method_name_array_list){

            // Build a method from name -
            Method method_instance = _delegate_object.getClass().getMethod(method_name,model_wrapper.getClass(),_transformation_properties_tree.getClass());

            // call the method -
            String buffer = (String)method_instance.invoke(_delegate_object,model_wrapper,_transformation_properties_tree);

            // lookup the output path -
            String fully_qualified_path = path_map.get(method_name);

            // write the file -
            write(fully_qualified_path,buffer);
        }


        // Build the driver -
        //String fully_qualified_driver_path =
        //String driver_buffer = _delegate_object.buildDriverFunctionBuffer(model_wrapper,_transformation_properties_tree);
        //write(fully_qualified_driver_path,driver_buffer);

        // Build the data file -

    }

    @Override
    public void setPropertiesTree(VLCGTransformationPropertyTree properties_tree) {

        if (properties_tree == null){
            return;
        }

        _transformation_properties_tree = properties_tree;
    }


    // private methods -
    private void write(String path,String buffer) throws Exception {

        // Create writer
        File oFile = new File(path);
        BufferedWriter writer = new BufferedWriter(new FileWriter(oFile));

        // Write buffer to file system and close writer
        writer.write(buffer);
        writer.close();
    }
}
