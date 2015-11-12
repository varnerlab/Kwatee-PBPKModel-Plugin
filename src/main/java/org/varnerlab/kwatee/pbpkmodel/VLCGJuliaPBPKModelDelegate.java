package org.varnerlab.kwatee.pbpkmodel;

import org.varnerlab.kwatee.foundation.VLCGCopyrightFactory;
import org.varnerlab.kwatee.foundation.VLCGTransformationPropertyTree;
import org.varnerlab.kwatee.pbpkmodel.model.VLCGPBPKBiochemistryControlModel;
import org.varnerlab.kwatee.pbpkmodel.model.VLCGPBPKCompartmentModel;
import org.varnerlab.kwatee.pbpkmodel.model.VLCGPBPKSpeciesModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.jar.Pack200;

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
public class VLCGJuliaPBPKModelDelegate {

    // instance variables -
    private VLCGCopyrightFactory copyrightFactory = VLCGCopyrightFactory.getSharedInstance();
    private java.util.Date today = Calendar.getInstance().getTime();
    private SimpleDateFormat date_formatter = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

    private String _generateSpeciesAliasListForModelTree(VLCGPBPKModelTreeWrapper model_tree) throws Exception {

        StringBuilder buffer = new StringBuilder();

        // write out the species list -
        ArrayList<VLCGPBPKSpeciesModel> species_model_array = model_tree.getSpeciesModelsFromPBPKModelTree();
        int species_index = 1;
        String local_compartment_name = model_tree.getFirstCompartmentNameFromPBPKModelTree();
        buffer.append("# ");
        buffer.append(local_compartment_name);
        buffer.append("\n");
        for (VLCGPBPKSpeciesModel species_model : species_model_array){

            // Get data from the model -
            String symbol = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);
            String compartment_name = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_COMPARTMENT);
            String species_type = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SPECIES_TYPE);

            if (local_compartment_name.equalsIgnoreCase(compartment_name) == false && species_type.equalsIgnoreCase("biochemical")){
                //buffer.append("# ------------------------------------------------------------------------------------------------ #\n");
                buffer.append("\n");
                buffer.append("# ");
                buffer.append(compartment_name);
                buffer.append("\n");
            }


            if (species_type.equalsIgnoreCase("biochemical") == true){

                // write the line -
                buffer.append(symbol);
                buffer.append("_");
                buffer.append(compartment_name);
                buffer.append(" = ");
                buffer.append("x[");
                buffer.append(species_index);
                buffer.append("];\n");

            }
            else {

                // write the line -
                buffer.append(symbol);
                buffer.append(" = ");
                buffer.append("x[");
                buffer.append(species_index);
                buffer.append("];\n");
            }

            // update the species index -
            species_index++;

            local_compartment_name = compartment_name;
        }

        return buffer.toString();
    }

    public String _generateSpeciesInputAliasListForModelTree(VLCGPBPKModelTreeWrapper model_tree) throws Exception {

        // Method variables -
        StringBuilder buffer = new StringBuilder();

        // Get the array from the data dictionary -
        buffer.append("input_concentration_array = data_dictionary[\"INPUT_CONCENTRATION_ARRAY\"]\n");

        ArrayList<VLCGPBPKSpeciesModel> species_model_array = model_tree.getSpeciesModelsFromPBPKModelTree();
        int input_concentration_index = 1;
        for (VLCGPBPKSpeciesModel species_model : species_model_array) {

            String species_symbol = (String) species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);

            // ok, we the species -
            String species_type = (String) species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SPECIES_TYPE);
            if (species_type.equalsIgnoreCase("volume") == false) {

                String home_compartment_symbol = (String) species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_COMPARTMENT);
                ArrayList<String> upstream_compartment_symbol_array = model_tree.getListOfCompartmentsUpstreamOfCompartmentWithSymbol(home_compartment_symbol);
                for (String upstream_compartment : upstream_compartment_symbol_array) {

                    // ok, if we have an upstream component that is [] -> we have a souce term
                    if (upstream_compartment.equalsIgnoreCase("[]")) {

                        // ok, generate an input species by default -
                        String special_symbol = species_symbol + "_input_" + home_compartment_symbol;

                        buffer.append(special_symbol);
                        buffer.append(" = ");
                        buffer.append("input_concentration_array[");
                        buffer.append(input_concentration_index++);
                        buffer.append("];\n");
                    }
                }
            }
        }

        // return -
        return buffer.toString();
    }

    public String _generateSpeciesVolumeListForModelTree(VLCGPBPKModelTreeWrapper model_tree) throws Exception {

        // Method variables -
        StringBuilder buffer = new StringBuilder();

        // Get species list -
        ArrayList<VLCGPBPKSpeciesModel> species_model_array = model_tree.getSpeciesModelsFromPBPKModelTree();

        // declare volume compartment array -
        buffer.append("local_volume_array = Float64[];\n");

        // how many biochemical species do we have?
        int number_of_biochemical_species = model_tree.calculateTheNumberOfBiochemicalSpecies();
        int species_index = 1;
        for (VLCGPBPKSpeciesModel species_model : species_model_array){

            // Get species type -
            String species_type = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SPECIES_TYPE);
            if (species_type.equalsIgnoreCase("volume") == false){

                // ok - get the compartment for this species -
                String home_compartment = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_COMPARTMENT);

                // get index for home compartment -
                int compartment_index = model_tree.findIndexOfCompartmentWithSymbol(home_compartment);

                String volume_symbol = "volume_"+home_compartment;
                String species_symbol = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);

                // write the line ..
                buffer.append("# ");
                buffer.append(species_index++);
                buffer.append(" ");
                buffer.append(species_symbol);
                buffer.append(" ");
                buffer.append(home_compartment);
                buffer.append("\n");
                buffer.append(volume_symbol);
                buffer.append(" = ");
                buffer.append(" x[");
                buffer.append(number_of_biochemical_species+compartment_index);
                buffer.append("];\n");
                buffer.append("push!(local_volume_array,");
                buffer.append(volume_symbol);
                buffer.append(");\n");
                buffer.append("\n");
            }
        }


        // return -
        return buffer.toString();
    }


    public String buildStoichiometricMatrixBuffer(VLCGPBPKModelTreeWrapper model_tree, VLCGTransformationPropertyTree property_tree) throws Exception {

        // Method variables -
        StringBuilder buffer = new StringBuilder();

        // stoichiometric matrix is NSPECIES x NREACTIONS big -
        ArrayList<VLCGPBPKSpeciesModel> species_model_array = model_tree.getSpeciesModelsFromPBPKModelTree();
        for (VLCGPBPKSpeciesModel species_model : species_model_array){

            // Get species symbol, and home compartment -
            String species_symbol = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);
            String home_compartment_symbol = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_COMPARTMENT);
            String species_type = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SPECIES_TYPE);
            if (species_type.equalsIgnoreCase("volume") == false){

                // ok, we have a biochemical species -
                String row_string = model_tree.getStoichiometricCoefficientsForSpeciesInCompartment(species_symbol,home_compartment_symbol);
                buffer.append(row_string);
            }
        }

        return buffer.toString();
    }

    public String buildCompartmentConnectivityMatrixBuffer(VLCGPBPKModelTreeWrapper model_tree, VLCGTransformationPropertyTree property_tree) throws Exception {

        // Method variables -
        StringBuilder buffer = new StringBuilder();


        // get the compartment model array -
        ArrayList<VLCGPBPKCompartmentModel> compartment_model_array = model_tree.getCompartmentModelsFromPBPKModelTree();
        for (VLCGPBPKCompartmentModel compartment_model : compartment_model_array){

            // Get the compartment symbol -
            String compartment_symbol = (String)compartment_model.getModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL);

            // Build the row -
            String row_string = model_tree.getConnectionsForCompartmentWithSymbol(compartment_symbol);
            buffer.append(row_string);
        }

        return buffer.toString();
    }

    public String buildHeartRateFunctionBuffer(VLCGPBPKModelTreeWrapper model_tree, VLCGTransformationPropertyTree property_tree) throws Exception {

        // Method variables -
        StringBuilder buffer = new StringBuilder();

        // Get the control function name -
        String heart_rate_function_name = property_tree.lookupKwateeHeartRateFunctionName();

        // Copyright notice -
        String copyright = copyrightFactory.getJuliaCopyrightHeader();
        buffer.append(copyright);

        // Fill in the buffer -
        buffer.append("function ");
        buffer.append(heart_rate_function_name);
        buffer.append("(t,x,beats_per_minute,stroke_volume,data_dictionary)\n");
        buffer.append("# ---------------------------------------------------------------------- #\n");
        buffer.append("# ");
        buffer.append(heart_rate_function_name);
        buffer.append(".jl was generated using the Kwatee code generation system.\n");
        buffer.append("# Username: ");
        buffer.append(property_tree.lookupKwateeModelUsername());
        buffer.append("\n");
        buffer.append("# Type: ");
        buffer.append(property_tree.lookupKwateeModelType());
        buffer.append("\n");
        buffer.append("# Version: ");
        buffer.append(property_tree.lookupKwateeModelVersion());
        buffer.append("\n");
        buffer.append("# Generation timestamp: ");
        buffer.append(date_formatter.format(today));
        buffer.append("\n");
        buffer.append("# \n");
        buffer.append("# Arguments: \n");
        buffer.append("# t  - current time \n");
        buffer.append("# x  - state vector \n");
        buffer.append("# beats_per_minute - heart beats per minute\n");
        buffer.append("# stoke_volume - stroke volume per beat\n");
        buffer.append("# data_dictionary  - Data dictionary instance (holds model parameters) \n");
        buffer.append("# ---------------------------------------------------------------------- #\n");
        buffer.append("\n");

        // Alias the species vector -
        buffer.append("# Alias the species vector - \n");
        buffer.append(_generateSpeciesAliasListForModelTree(model_tree));
        buffer.append("\n");

        buffer.append("# Update the beats_per_minute - \n");
        buffer.append("beats_per_minute = beats_per_minute;\n");
        buffer.append("\n");

        buffer.append("# Update the stroke volume - \n");
        buffer.append("stroke_volume = stroke_volume;\n");
        buffer.append("\n");

        // write the return -
        buffer.append("return (beats_per_minute,stroke_volume);\n");
        buffer.append("end;\n");

        // return -
        return buffer.toString();
    }

    public String buildCardiacDistributionFunctionBuffer(VLCGPBPKModelTreeWrapper model_tree, VLCGTransformationPropertyTree property_tree) throws Exception {

        // Method variables -
        StringBuilder buffer = new StringBuilder();

        // Get the control function name -
        String cd_rate_function_name = property_tree.lookupKwateeCardiacDistributionFunctionName();

        // Copyright notice -
        String copyright = copyrightFactory.getJuliaCopyrightHeader();
        buffer.append(copyright);

        // Fill in the buffer -
        buffer.append("function ");
        buffer.append(cd_rate_function_name);
        buffer.append("(t,x,default_flow_parameter_array,data_dictionary)\n");
        buffer.append("# ---------------------------------------------------------------------- #\n");
        buffer.append("# ");
        buffer.append(cd_rate_function_name);
        buffer.append(".jl was generated using the Kwatee code generation system.\n");
        buffer.append("# Username: ");
        buffer.append(property_tree.lookupKwateeModelUsername());
        buffer.append("\n");
        buffer.append("# Type: ");
        buffer.append(property_tree.lookupKwateeModelType());
        buffer.append("\n");
        buffer.append("# Version: ");
        buffer.append(property_tree.lookupKwateeModelVersion());
        buffer.append("\n");
        buffer.append("# Generation timestamp: ");
        buffer.append(date_formatter.format(today));
        buffer.append("\n");
        buffer.append("# \n");
        buffer.append("# Arguments: \n");
        buffer.append("# t  - current time \n");
        buffer.append("# x  - state vector \n");
        buffer.append("# default_flow_parameter_array - default flow parameters \n");
        buffer.append("# data_dictionary  - Data dictionary instance (holds model parameters) \n");
        buffer.append("# ---------------------------------------------------------------------- #\n");
        buffer.append("\n");

        // Alias the species vector -
        buffer.append("# Alias the species vector - \n");
        buffer.append(_generateSpeciesAliasListForModelTree(model_tree));
        buffer.append("\n");

        // update -
        buffer.append("# Update the flow parameter array - \n");
        buffer.append("flow_parameter_array = default_flow_parameter_array");
        buffer.append("\n");

        // write the return statement -
        buffer.append("return (flow_parameter_array);\n");
        buffer.append("end;\n");

        // return -
        return buffer.toString();
    }

    public String buildCompartmentFlowFunctionBuffer(VLCGPBPKModelTreeWrapper model_tree, VLCGTransformationPropertyTree property_tree) throws Exception {

        // Method variables -
        StringBuilder buffer = new StringBuilder();

        // Get the control function name -
        String flow_function_name = property_tree.lookupKwateeCompartmentFlowFunctionName();

        // Get/Set the heart rate function import -
        String hr_function_name = property_tree.lookupKwateeHeartRateFunctionName();
        buffer.append("include(\"");
        buffer.append(hr_function_name);
        buffer.append(".jl\");\n");

        // Get/Set the cardiac distribution function import -
        String cardiac_distribution_function_name = property_tree.lookupKwateeCardiacDistributionFunctionName();
        buffer.append("include(\"");
        buffer.append(cardiac_distribution_function_name);
        buffer.append(".jl\");\n");

        // Copyright notice -
        String copyright = copyrightFactory.getJuliaCopyrightHeader();
        buffer.append(copyright);

        // Fill in the buffer -
        buffer.append("function ");
        buffer.append(flow_function_name);
        buffer.append("(t,x,data_dictionary)\n");
        buffer.append("# ---------------------------------------------------------------------- #\n");
        buffer.append("# ");
        buffer.append(flow_function_name);
        buffer.append(".jl was generated using the Kwatee code generation system.\n");
        buffer.append("# Username: ");
        buffer.append(property_tree.lookupKwateeModelUsername());
        buffer.append("\n");
        buffer.append("# Type: ");
        buffer.append(property_tree.lookupKwateeModelType());
        buffer.append("\n");
        buffer.append("# Version: ");
        buffer.append(property_tree.lookupKwateeModelVersion());
        buffer.append("\n");
        buffer.append("# Generation timestamp: ");
        buffer.append(date_formatter.format(today));
        buffer.append("\n");
        buffer.append("# \n");
        buffer.append("# Arguments: \n");
        buffer.append("# t  - current time \n");
        buffer.append("# x  - state vector \n");
        buffer.append("# data_dictionary  - Data dictionary instance (holds model parameters) \n");
        buffer.append("# ---------------------------------------------------------------------- #\n");
        buffer.append("\n");

        // Alias the characteristic data -
        buffer.append("# Characteristic variables - \n");
        buffer.append("characteristic_variable_array = data_dictionary[\"CHARACTERISTIC_VARIABLE_ARRAY\"];\n");
        buffer.append("characteristic_flow_rate = characteristic_variable_array[3];\n");
        buffer.append("\n");

        // Alias the species vector -
        buffer.append("# Alias the species vector - \n");
        buffer.append(_generateSpeciesAliasListForModelTree(model_tree));
        buffer.append("\n");

        // Alias the input vector -
        buffer.append("# Alias the species input vector - \n");
        buffer.append(_generateSpeciesInputAliasListForModelTree(model_tree));
        buffer.append("\n");

        // Get some stuff we'll need from the data dictonary -
        buffer.append("# Get data we need from the data_dictionary - \n");
        buffer.append("default_bpm = data_dictionary[\"DEFAULT_BEATS_PER_MINUTE\"];\n");
        buffer.append("default_stroke_volume = data_dictionary[\"DEFAULT_STROKE_VOLUME\"];\n");
        buffer.append("default_flow_parameter_array = data_dictionary[\"FLOW_PARAMETER_ARRAY\"];\n");
        buffer.append("\n");

        // update the sv and bpm -
        buffer.append("# Update the heart rate, and stroke volume - \n");
        buffer.append("(bpm,stroke_volume) = ");
        buffer.append(hr_function_name);
        buffer.append("(t,x,default_bpm,default_stroke_volume,data_dictionary);\n");

        // upadate the distribution -
        buffer.append("\n");
        buffer.append("# Update the fraction of cardiac output going to each organ - \n");
        buffer.append("(flow_parameter_array) = ");
        buffer.append(cardiac_distribution_function_name);
        buffer.append("(t,x,default_flow_parameter_array,data_dictionary);\n");

        // calculate the q-vector -
        buffer.append("\n");
        buffer.append("# Calculate the q_vector - \n");
        buffer.append("q_vector = Float64[];\n");
        buffer.append("cardiac_output = (1.0/characteristic_flow_rate)*bpm*stroke_volume;\n");
        buffer.append("\n");

        // Get list of names -
        ArrayList<String> connection_name_list = model_tree.getCompartmentConnectionNamesFromPBPKModelTree();
        int connection_index = 1;
        for (String connection_name : connection_name_list){


            // ok, write the record -
            buffer.append("# ");
            buffer.append(connection_index);
            buffer.append(" ");
            buffer.append(connection_name);
            buffer.append("\n");

            if (model_tree.isThisASourceCompartmentConnection(connection_name) == true){

                buffer.append("tmp_flow_rate = flow_parameter_array[");
                buffer.append(connection_index);
                buffer.append("];\n");
                buffer.append("push!(q_vector,tmp_flow_rate);\n");
                buffer.append("\n");
            }
            else {

                buffer.append("tmp_flow_rate = cardiac_output*");
                buffer.append("flow_parameter_array[");
                buffer.append(connection_index);
                buffer.append("];\n");
                buffer.append("push!(q_vector,tmp_flow_rate);\n");
                buffer.append("\n");
            }

            // update the index -
            connection_index++;
        }

        // calculate the q-vector -
        buffer.append("# Calculate the species_flow_terms - \n");
        buffer.append("species_flow_vector = Float64[];\n");
        buffer.append("\n");

        // Get list of species models -
        ArrayList<VLCGPBPKSpeciesModel> species_model_array = model_tree.getSpeciesModelsFromPBPKModelTree();
        for (VLCGPBPKSpeciesModel species_model : species_model_array){

            String species_symbol = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);

            // ok, we the species -
            String species_type = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SPECIES_TYPE);
            if (species_type.equalsIgnoreCase("volume") == false){

                String home_compartment_symbol = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_COMPARTMENT);

                // ok, we have a biochemical species -
                buffer.append("# ");
                buffer.append(species_symbol);
                buffer.append("_");
                buffer.append(home_compartment_symbol);
                buffer.append(" ---------- \n");

                // if we have a *no-fly* species, then just put a zero for this flow -
                if (model_tree.isSpeciesWithSymbolSubjectToRuleWithName(species_symbol,"fixed")){

                    // ooops!! we have a no-fly species ...
                    buffer.append("push!(species_flow_vector,0.0);\n");
                    buffer.append("\n");
                }
                else {

                    // What compartment is this species in?
                    buffer.append("tmp_flow_term = -(");

                    // What comparments are *downstream* of this compartment?
                    ArrayList<String> downstream_compartment_symbol_array = model_tree.getListOfCompartmentsDownstreamOfCompartmentWithSymbol(home_compartment_symbol);

                    // Iterate through the connections -
                    int number_of_downstream_connections = downstream_compartment_symbol_array.size();
                    int downstream_connection_counter = 0;
                    for (String downstream_compartment : downstream_compartment_symbol_array){

                        // lookup the index of the flow -
                        int flow_index = model_tree.findIndexOfConnectionBetweenStartAndEndCompartments(home_compartment_symbol,downstream_compartment);

                        // write the line -
                        buffer.append("q_vector[");
                        buffer.append(flow_index);
                        buffer.append("]");


                        if (downstream_connection_counter<number_of_downstream_connections-1){
                            buffer.append("+");
                        }

                        // update -
                        downstream_connection_counter++;
                    }

                    buffer.append(")*");
                    buffer.append(species_symbol);
                    buffer.append("_");
                    buffer.append(home_compartment_symbol);

                    // ok, we are down w/the outflows .. no process the inflow -
                    ArrayList<String> upstream_compartment_symbol_array = model_tree.getListOfCompartmentsUpstreamOfCompartmentWithSymbol(home_compartment_symbol);
                    // Iterate through the connections -
                    int number_of_upstream_connections = upstream_compartment_symbol_array.size();
                    int upstream_connection_counter = 0;

                    if (number_of_upstream_connections>0){
                        buffer.append("+(");
                    }

                    for (String upstream_compartment : upstream_compartment_symbol_array){

                        // lookup the index of the flow -
                        int flow_index = model_tree.findIndexOfConnectionBetweenStartAndEndCompartments(upstream_compartment,home_compartment_symbol);
                        buffer.append("q_vector[");
                        buffer.append(flow_index);
                        buffer.append("]*");

                        // ok, if we have an upstream component that is [] -> we have a souce term
                        if (upstream_compartment.equalsIgnoreCase("[]")){

                            // ok, generate an input species by default -
                            String special_symbol = species_symbol+"_input_"+home_compartment_symbol;
                            buffer.append(special_symbol);
                        }
                        else {

                            // write the line -
                            buffer.append(species_symbol);
                            buffer.append("_");
                            buffer.append(upstream_compartment);
                        }

                        if (upstream_connection_counter<number_of_upstream_connections-1){
                            buffer.append("+");
                        }

                        // update -
                        upstream_connection_counter++;
                    }

                    // ok, we are done with the inflows .. write the endline -
                    buffer.append(");\n");
                    buffer.append("push!(species_flow_vector,");//tmp_flow_term);\n");
                    buffer.append("(1.0/volume_");
                    buffer.append(home_compartment_symbol);
                    buffer.append(")*tmp_flow_term);\n");
                    buffer.append("tmp_flow_term = 0;\n");
                    buffer.append("\n");
                }
            }
        }

        // write the return statement -
        buffer.append("return (species_flow_vector,q_vector);\n");
        buffer.append("end;\n");

        // return -
        return buffer.toString();
    }


    public String buildBalanceFunctionBuffer(VLCGPBPKModelTreeWrapper model_tree, VLCGTransformationPropertyTree property_tree) throws Exception {

        // Method variables -
        StringBuilder massbalances = new StringBuilder();

        // Get the balance function name -
        String balance_function_name = property_tree.lookupKwateeBalanceFunctionName();

        // Get/Set the kinetics function import -
        String kinetics_function_name = property_tree.lookupKwateeKineticsFunctionName();
        massbalances.append("include(\"");
        massbalances.append(kinetics_function_name);
        massbalances.append(".jl\");\n");

        // Get/Set the kinetics function import -
        String control_function_name = property_tree.lookupKwateeControlFunctionName();
        massbalances.append("include(\"");
        massbalances.append(control_function_name);
        massbalances.append(".jl\");\n");

        // Get/Set the flow function import -
        String flow_function_name = property_tree.lookupKwateeCompartmentFlowFunctionName();
        massbalances.append("include(\"");
        massbalances.append(flow_function_name);
        massbalances.append(".jl\");\n");
        massbalances.append("\n");

        // Copyright notice -
        String copyright = copyrightFactory.getJuliaCopyrightHeader();
        massbalances.append(copyright);

        // Fill in the buffer -
        massbalances.append("function ");
        massbalances.append(balance_function_name);
        massbalances.append("(t,x,dxdt_vector,data_dictionary)\n");
        massbalances.append("# ---------------------------------------------------------------------- #\n");
        massbalances.append("# ");
        massbalances.append(balance_function_name);
        massbalances.append(".jl was generated using the Kwatee code generation system.\n");
        massbalances.append("# Username: ");
        massbalances.append(property_tree.lookupKwateeModelUsername());
        massbalances.append("\n");
        massbalances.append("# Type: ");
        massbalances.append(property_tree.lookupKwateeModelType());
        massbalances.append("\n");
        massbalances.append("# Version: ");
        massbalances.append(property_tree.lookupKwateeModelVersion());
        massbalances.append("\n");
        massbalances.append("# Generation timestamp: ");
        massbalances.append(date_formatter.format(today));
        massbalances.append("\n");
        massbalances.append("# \n");
        massbalances.append("# Arguments: \n");
        massbalances.append("# t  - current time \n");
        massbalances.append("# x  - state vector \n");
        massbalances.append("# dxdt_vector - right hand side vector \n");
        massbalances.append("# data_dictionary  - Data dictionary instance (holds model parameters) \n");
        massbalances.append("# ---------------------------------------------------------------------- #\n");
        massbalances.append("\n");
        massbalances.append("# Get data from the data_dictionary - \n");
        massbalances.append("S = data_dictionary[\"STOICHIOMETRIC_MATRIX\"];\n");
        massbalances.append("C = data_dictionary[\"FLOW_CONNECTIVITY_MATRIX\"];\n");
        massbalances.append("tau_array = data_dictionary[\"TIME_CONSTANT_ARRAY\"];\n");
        massbalances.append("\n");

        massbalances.append("# Correct nagative x's = throws errors in control even if small - \n");
        massbalances.append("idx = find(x->(x<0),x);\n");
        massbalances.append("x[idx] = 0.0;\n");
        massbalances.append("\n");
        //massbalances.append("# Alias compartment volumes - \n");
        //massbalances.append(_generateSpeciesVolumeListForModelTree(model_tree));

        massbalances.append("# Call the kinetics function - \n");
        massbalances.append("(rate_vector) = ");
        massbalances.append(kinetics_function_name);
        massbalances.append("(t,x,data_dictionary);\n");

        // call the control function -
        massbalances.append("\n");
        massbalances.append("# Call the control function - \n");
        massbalances.append("(rate_vector) = ");
        massbalances.append(control_function_name);
        massbalances.append("(t,x,rate_vector,data_dictionary);\n");

        // Call the flow function -
        massbalances.append("\n");
        massbalances.append("# Call the flow function - \n");
        massbalances.append("(flow_terms_vector,q_vector) = ");
        massbalances.append(flow_function_name);
        massbalances.append("(t,x,data_dictionary);\n");
        massbalances.append("\n");

        // check - is this model large scale optimized?
        if (property_tree.isKwateeModelLargeScaleOptimized() == true){

            // build explicit list of balance equations -

        }
        else {

            // balance are encoded as matrix vector product -
            massbalances.append("# Encode the biochemical balance equations as a matrix vector product - \n");
            massbalances.append("tmp_vector = flow_terms_vector + S*rate_vector;\n");
            massbalances.append("number_of_states = length(tmp_vector);\n");
            massbalances.append("for state_index in [1:number_of_states]\n");
            massbalances.append("\tdxdt_vector[state_index] = tau_array[state_index]*tmp_vector[state_index];\n");
            massbalances.append("end\n");
            massbalances.append("\n");
            massbalances.append("# Encode the volume balance equations as a matrix vector product - \n");
            massbalances.append("tmp_dvdt_vector = C*q_vector;\n");
            massbalances.append("number_of_compartments = length(tmp_dvdt_vector);\n");
            massbalances.append("for compartment_index in [1:number_of_compartments]\n");
            massbalances.append("\tstate_vector_index = (number_of_states)+compartment_index;\n");
            massbalances.append("\tdxdt_vector[state_vector_index] = tmp_dvdt_vector[compartment_index];\n");
            massbalances.append("end\n");
        }

        // last line -
        massbalances.append("\n");
        massbalances.append("end\n");

        // return the buffer -
        return massbalances.toString();
    }

    public String buildControlFunctionBuffer(VLCGPBPKModelTreeWrapper model_tree,VLCGTransformationPropertyTree property_tree) throws Exception {

        // Method variables -
        StringBuilder buffer = new StringBuilder();

        // Get the control function name -
        String control_function_name = property_tree.lookupKwateeControlFunctionName();

        // Copyright notice -
        String copyright = copyrightFactory.getJuliaCopyrightHeader();
        buffer.append(copyright);

        // Fill in the buffer -
        buffer.append("function ");
        buffer.append(control_function_name);
        buffer.append("(t,x,rate_vector,data_dictionary)\n");
        buffer.append("# ---------------------------------------------------------------------- #\n");
        buffer.append("# ");
        buffer.append(control_function_name);
        buffer.append(".jl was generated using the Kwatee code generation system.\n");
        buffer.append("# Username: ");
        buffer.append(property_tree.lookupKwateeModelUsername());
        buffer.append("\n");
        buffer.append("# Type: ");
        buffer.append(property_tree.lookupKwateeModelType());
        buffer.append("\n");
        buffer.append("# Version: ");
        buffer.append(property_tree.lookupKwateeModelVersion());
        buffer.append("\n");
        buffer.append("# Generation timestamp: ");
        buffer.append(date_formatter.format(today));
        buffer.append("\n");
        buffer.append("# \n");
        buffer.append("# Arguments: \n");
        buffer.append("# t  - current time \n");
        buffer.append("# x  - state vector \n");
        buffer.append("# rate_vector - vector of reaction rates \n");
        buffer.append("# data_dictionary  - Data dictionary instance (holds model parameters) \n");
        buffer.append("# ---------------------------------------------------------------------- #\n");
        buffer.append("\n");
        buffer.append("# Set a default value for the allosteric control variables - \n");
        buffer.append("EPSILON = 1.0e-3;\n");
        buffer.append("number_of_reactions = length(rate_vector);\n");
        buffer.append("control_vector = ones(number_of_reactions);\n");
        buffer.append("control_parameter_array = data_dictionary[\"CONTROL_PARAMETER_ARRAY\"];\n");
        buffer.append("\n");

        // Alias the species vector -
        buffer.append("# Alias the species vector - \n");
        buffer.append(_generateSpeciesAliasListForModelTree(model_tree));
        buffer.append("\n");


        // Formulate/write the control models -
        ArrayList<VLCGPBPKCompartmentModel> compartment_model_array = model_tree.getCompartmentModelsFromPBPKModelTree();
        int control_index = 1;
        int reaction_index = 1;
        for (VLCGPBPKCompartmentModel compartment_model : compartment_model_array) {

            // Get the symbol of the compartment -
            String compartment_symbol = (String) compartment_model.getModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL);

            // Get the list of targets in this compartment -
            ArrayList<String> control_target_array = model_tree.getBiochemistryControlTargetsFromPBPKModelTreeForCompartmentWithSymbol(compartment_symbol);

            for (String control_target : control_target_array){

                // Get the control model for this compartment and target -
                ArrayList<VLCGPBPKBiochemistryControlModel> control_model_array = model_tree.getBiochemistryControlModelFromPBPKModelTreeForCompartmentWithSymbol(compartment_symbol,control_target);

                buffer.append("# ----------------------------------------------------------------------------------- #\n");
                buffer.append("transfer_function_vector = Float64[];\n");
                buffer.append("\n");

                // does this contain an inhibitor step?
                Boolean contains_inhibitor = false;
                // if control_model_array has more than one element, then we have a target with multipe inputs ...
                for (VLCGPBPKBiochemistryControlModel control_model : control_model_array) {

                    // Get the control comment string -
                    String comment_string = (String) control_model.getModelComponent(VLCGPBPKBiochemistryControlModel.FORMATTED_RAW_RECORD);
                    String raw_actor = (String) control_model.getModelComponent(VLCGPBPKBiochemistryControlModel.BIOCHEMISTRY_CONTROL_ACTOR);
                    String target = (String) control_model.getModelComponent(VLCGPBPKBiochemistryControlModel.BIOCHEMISTRY_CONTROL_TARGET);
                    String type = (String) control_model.getModelComponent(VLCGPBPKBiochemistryControlModel.BIOCHEMISTRY_CONTROL_TYPE);

                    buffer.append("# ");
                    buffer.append(comment_string);
                    buffer.append("\n");

                    String actor = raw_actor+"_"+compartment_symbol;

                    if (type.equalsIgnoreCase("repression") || type.equalsIgnoreCase("inhibition")){

                        contains_inhibitor = true;

                        // check do we have a zero inhibitor?
                        buffer.append("if (");
                        buffer.append(actor);
                        buffer.append("<EPSILON);\n");
                        buffer.append("\tpush!(transfer_function_vector,1.0);\n");
                        buffer.append("else\n");
                        buffer.append("\tpush!(transfer_function_vector,1.0 - (control_parameter_array[");
                        buffer.append(control_index);
                        buffer.append(",1]*(");
                        buffer.append(actor);
                        buffer.append(")^control_parameter_array[");
                        buffer.append(control_index);
                        buffer.append(",2])/(1+");
                        buffer.append("control_parameter_array[");
                        buffer.append(control_index);
                        buffer.append(",1]*(");
                        buffer.append(actor);
                        buffer.append(")^control_parameter_array[");
                        buffer.append(control_index);
                        buffer.append(",2]));\n");
                        buffer.append("end\n");
                        buffer.append("\n");
                    }
                    else {

                        // write -
                        buffer.append("push!(transfer_function_vector,(control_parameter_array[");
                        buffer.append(control_index);
                        buffer.append(",1]*(");
                        buffer.append(actor);
                        buffer.append(")^control_parameter_array[");
                        buffer.append(control_index);
                        buffer.append(",2])/(1+");
                        buffer.append("control_parameter_array[");
                        buffer.append(control_index);
                        buffer.append(",1]*(");
                        buffer.append(actor);
                        buffer.append(")^control_parameter_array[");
                        buffer.append(control_index);
                        buffer.append(",2]));\n");
                        buffer.append("\n");
                    }

                    // update -
                    control_index++;
                }

                reaction_index = model_tree.findIndexForReactionWithNameInCompartment(control_target,compartment_symbol);

                // ok, the default is the maximum - but if we have inhibitor/repression reactions we
                // use min -
                if (contains_inhibitor == true){

                    // integrate the transfer functions -
                    buffer.append("control_vector[");
                    buffer.append(reaction_index);
                    buffer.append("] = minimum(transfer_function_vector);\n");
                    buffer.append("transfer_function_vector = 0;\n");
                    buffer.append("# ----------------------------------------------------------------------------------- #\n");
                    buffer.append("\n");
                }
                else {

                    // integrate the transfer functions -
                    buffer.append("control_vector[");
                    buffer.append(reaction_index);
                    buffer.append("] = maximum(transfer_function_vector);\n");
                    buffer.append("transfer_function_vector = 0;\n");
                    buffer.append("# ----------------------------------------------------------------------------------- #\n");
                    buffer.append("\n");
                }
            }
        }


        buffer.append("# Modify the rate_vector with the control variables - \n");
        buffer.append("rate_vector = rate_vector.*control_vector;\n");

        // last line -
        buffer.append("\n");
        buffer.append("# Return the modified rate vector - \n");
        buffer.append("return rate_vector;\n");
        buffer.append("end\n");

        // return the buffer -
        return buffer.toString();
    }

    public String buildKineticsFunctionBuffer(VLCGPBPKModelTreeWrapper model_tree, VLCGTransformationPropertyTree property_tree) throws Exception {

        // String builder -
        StringBuilder buffer = new StringBuilder();

        // Copyright notice -
        String copyright = copyrightFactory.getJuliaCopyrightHeader();
        buffer.append(copyright);

        // Get the kinetics function name -
        String kinetics_function_name = property_tree.lookupKwateeKineticsFunctionName();

        // Propulate the buffer -
        buffer.append("function ");
        buffer.append(kinetics_function_name);
        buffer.append("(t,x,data_dictionary)\n");
        buffer.append("# --------------------------------------------------------------------- #\n");
        buffer.append("# ");
        buffer.append(kinetics_function_name);
        buffer.append(".jl was generated using the Kwatee code generation system.\n");
        buffer.append("# Username: ");
        buffer.append(property_tree.lookupKwateeModelUsername());
        buffer.append("\n");
        buffer.append("# Type: ");
        buffer.append(property_tree.lookupKwateeModelType());
        buffer.append("\n");
        buffer.append("# Version: ");
        buffer.append(property_tree.lookupKwateeModelVersion());
        buffer.append("\n");
        buffer.append("# Generation timestamp: ");
        buffer.append(date_formatter.format(today));
        buffer.append("\n");
        buffer.append("# \n");
        buffer.append("# Input arguments: \n");
        buffer.append("# t  - current time \n");
        buffer.append("# x  - state vector \n");
        buffer.append("# data_dictionary - parameter vector \n");
        buffer.append("# \n");
        buffer.append("# Return arguments: \n");
        buffer.append("# rate_vector - rate vector \n");
        buffer.append("# --------------------------------------------------------------------- #\n");
        buffer.append("# \n");
        buffer.append("# Alias the species vector - \n");
        buffer.append(_generateSpeciesAliasListForModelTree(model_tree));
        buffer.append("\n");

        // Alias the characteristic data -
        buffer.append("# Characteristic variables - \n");
        buffer.append("characteristic_variable_array = data_dictionary[\"CHARACTERISTIC_VARIABLE_ARRAY\"];\n");
        buffer.append("characteristic_concentration = characteristic_variable_array[2];\n");
        buffer.append("characteristic_time = characteristic_variable_array[4];\n");
        buffer.append("\n");

        // Write out the kinetics vector -
        buffer.append("# Formulate the kinetic rate vector - \n");
        buffer.append("rate_constant_array = data_dictionary[\"RATE_CONSTANT_ARRAY\"];\n");
        buffer.append("saturation_constant_array = data_dictionary[\"SATURATION_CONSTANT_ARRAY\"];\n");
        buffer.append("rate_vector = Float64[];\n");
        buffer.append("\n");

        // Get the list of compartment models -
        ArrayList<VLCGPBPKCompartmentModel> compartment_model_array = model_tree.getCompartmentModelsFromPBPKModelTree();
        ArrayList<String> lookup_species_order_vector = model_tree.makeSpeciesCompartmentSymbolArrayForPBPKModelTree();
        int reaction_counter = 1;
        for (VLCGPBPKCompartmentModel compartment_model : compartment_model_array) {

            // Get the symbol of the compartment -
            String compartment_symbol = (String) compartment_model.getModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL);

            buffer.append("# -------------------------------------------------------------------------- # \n");
            buffer.append("# ");
            buffer.append(compartment_symbol);
            buffer.append("\n");
            buffer.append("# -------------------------------------------------------------------------- # \n");

            // ok, lookup the reactions that can occur in this compartment -
            ArrayList<String> reaction_name_array = model_tree.getReactionNamesFromPBPKModelTreeInCompartmentWithSymbol(compartment_symbol);
            for (String reaction_name : reaction_name_array) {

                // Get the raw string -
                String raw_string = model_tree.getRawReactionStringFromPBPKModelTreeForReactionWithNameAndCompartment(compartment_symbol, reaction_name);

                // write the comment line -
                buffer.append("# ");
                buffer.append(raw_string);
                buffer.append("\n");
                buffer.append("tmp_reaction = rate_constant_array[");
                buffer.append(reaction_counter);
                buffer.append("]");

                // do we have an enzyme?
                String enzyme_symbol = model_tree.getEnzymeSymbolFromPBPKModelTreeForReactionWithNameAndCompartment(compartment_symbol,reaction_name);
                if (enzyme_symbol.equalsIgnoreCase("[]") == false){

                    buffer.append("*(");
                    buffer.append(enzyme_symbol);
                    buffer.append("_");
                    buffer.append(compartment_symbol);
                    buffer.append(")");
                }

                // Get the reactants -
                // ok, if we have a source -or- degradation reaction, these are 0 and 1st order => no sat coefficient
                if (model_tree.isThisADegradationReaction(reaction_name,compartment_symbol) == false &&
                        model_tree.isThisASourceReaction(reaction_name,compartment_symbol) == false){

                    // ok - this reaction has MM kinetics -
                    ArrayList<VLCGPBPKSpeciesModel> reactant_array = model_tree.getReactantsForBiochemicalReactionWithNameInCompartment(reaction_name,compartment_symbol);
                    for (VLCGPBPKSpeciesModel local_species_model : reactant_array) {

                        // create the compound symbol -
                        String local_reactant_symbol = (String) local_species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);
                        String lookup_species_symbol = local_reactant_symbol+"::"+compartment_symbol;
                        String species_symbol = local_reactant_symbol+"_"+compartment_symbol;

                        // Get the index -
                        int local_species_index = lookup_species_order_vector.indexOf(lookup_species_symbol) + 1;

                        buffer.append("*((");
                        buffer.append(species_symbol);
                        buffer.append(")/(");
                        buffer.append("saturation_constant_array[");
                        buffer.append(reaction_counter);
                        buffer.append(",");
                        buffer.append(local_species_index);
                        buffer.append("] + ");
                        buffer.append(species_symbol);
                        buffer.append("))");
                    }

                    // add )
                    buffer.append(";\n");
                }
                else if (model_tree.isThisADegradationReaction(reaction_name,compartment_symbol) == true){

                    // ok - this is a degrdation reaction -
                    ArrayList<VLCGPBPKSpeciesModel> reactant_array = model_tree.getReactantsForBiochemicalReactionWithNameInCompartment(reaction_name,compartment_symbol);
                    for (VLCGPBPKSpeciesModel local_species_model : reactant_array) {

                        // create the compound symbol -
                        String local_reactant_symbol = (String) local_species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);
                        String species_symbol = local_reactant_symbol+"_"+compartment_symbol;
                        String stoichiometric_coefficient = (String)local_species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_COEFFICIENT);

                        if (stoichiometric_coefficient.equalsIgnoreCase("1.0") == false){

                            buffer.append("*((");
                            buffer.append(species_symbol);
                            buffer.append(")^");
                            buffer.append(stoichiometric_coefficient);
                            buffer.append(")");

                        }
                        else {

                            buffer.append("*(");
                            buffer.append(species_symbol);
                            buffer.append(")");
                        }
                    }

                    buffer.append(";\n");
                }
                else {

                    // ok - this is a source reaction -
                    buffer.append(";\n");
                }

                // write the push
                buffer.append("push!(rate_vector,tmp_reaction);\n");
                buffer.append("tmp_reaction = 0;\n");
                buffer.append("\n");

                // update the reaction counter -
                reaction_counter++;
            }
        }

        // last line -
        buffer.append("# return the kinetics vector -\n");
        buffer.append("return rate_vector;\n");
        buffer.append("end\n");

        // return the buffer -
        return buffer.toString();
    }


    public String buildDataDictionaryFunctionBuffer(VLCGPBPKModelTreeWrapper model_tree, VLCGTransformationPropertyTree property_tree) throws Exception {

        // String builder -
        StringBuilder buffer = new StringBuilder();

        // Copyright notice -
        String copyright = copyrightFactory.getJuliaCopyrightHeader();
        buffer.append(copyright);

        // Get the function name -
        String function_name = property_tree.lookupKwateeDataDictionaryFunctionName();
        buffer.append("function ");
        buffer.append(function_name);
        buffer.append("(TSTART,TSTOP,Ts)\n");

        buffer.append("# ----------------------------------------------------------------------------------- #\n");
        buffer.append("# ");
        buffer.append(function_name);
        buffer.append(".jl was generated using the Kwatee code generation system.\n");
        buffer.append("# ");
        buffer.append(function_name);
        buffer.append(": Stores model parameters as key - value pairs in a Julia Dict() \n");
        buffer.append("# Username: ");
        buffer.append(property_tree.lookupKwateeModelUsername());
        buffer.append("\n");
        buffer.append("# Type: ");
        buffer.append(property_tree.lookupKwateeModelType());
        buffer.append("\n");
        buffer.append("# Version: ");
        buffer.append(property_tree.lookupKwateeModelVersion());
        buffer.append("\n");
        buffer.append("# Generation timestamp: ");
        buffer.append(date_formatter.format(today));
        buffer.append("\n");
        buffer.append("# \n");
        buffer.append("# Input arguments: \n");
        buffer.append("# TSTART  - Time start \n");
        buffer.append("# TSTOP  - Time stop \n");
        buffer.append("# Ts - Time step \n");
        buffer.append("# \n");
        buffer.append("# Return arguments: \n");
        buffer.append("# data_dictionary  - Data dictionary instance (holds model parameters) \n");
        buffer.append("# ----------------------------------------------------------------------------------- #\n");
        buffer.append("\n");

        // flow related parameters -
        buffer.append("# Flow related parameters - \n");
        buffer.append("default_beats_per_minute = 100.0;\n");
        buffer.append("default_stroke_volume = 70*(1/1000);\n");
        buffer.append("flow_parameter_array = Float64[]\n");
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");

        // Get a list of connection names -
        ArrayList<String> flow_connection_name_array = model_tree.getCompartmentConnectionNamesFromPBPKModelTree();
        int connection_index = 1;
        for (String connection_name : flow_connection_name_array){

            // Get parameter value -
            String parameter_value = model_tree.getParameterValueFromCompartmentConnectionWithName(connection_name);

            // write line -
            String comment_line = model_tree.generateCommentForConnectionParameterForCompartmentConnectionWithName(connection_name);
            buffer.append("push!(flow_parameter_array,");
            buffer.append(parameter_value);
            buffer.append(");\t# ");
            buffer.append(connection_index);
            buffer.append("\t");
            buffer.append(comment_line);

            // update -
            connection_index++;
        }
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");


        // Last?? - Setup the characteristic concentration etc so we can make dimensionless equations -
        buffer.append("\n");
        buffer.append("# Characteristic variables array - \n");
        buffer.append("characteristic_variable_array = zeros(4);\n");
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");
        buffer.append("characteristic_volume = 1.0;\n");
        buffer.append("characteristic_concentration = 1.0;\n");
        buffer.append("characteristic_flow_rate = default_beats_per_minute*default_stroke_volume;\n");
        buffer.append("characteristic_time = characteristic_volume/characteristic_flow_rate;\n");
        buffer.append("characteristic_variable_array[1] = characteristic_volume;\n");
        buffer.append("characteristic_variable_array[2] = characteristic_concentration;\n");
        buffer.append("characteristic_variable_array[3] = characteristic_flow_rate;\n");
        buffer.append("characteristic_variable_array[4] = characteristic_time;\n");
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");
        buffer.append("\n");

        buffer.append("# Load the stoichiometric matrix - \n");

        // Get the path to the stoichiometric matrix -
        String fully_qualified_stoichiometric_matrix_path = property_tree.lookupKwateeStoichiometricMatrixFilePath();
        buffer.append("S = float(open(readdlm,");
        buffer.append("\"");
        buffer.append(fully_qualified_stoichiometric_matrix_path);
        buffer.append("\"));\n");
        buffer.append("(NSPECIES,NREACTIONS) = size(S);\n");
        buffer.append("\n");

        // Get/load the connectivity matrix -
        buffer.append("# Load the stoichiometric matrix - \n");
        String fully_qualified_connectivity_matrix_path = property_tree.lookupKwateeCompartmentConnectivityMatrixFilePath();
        buffer.append("C = float(open(readdlm,");
        buffer.append("\"");
        buffer.append(fully_qualified_connectivity_matrix_path);
        buffer.append("\"));\n");

        // build the initial_condition_array -
        buffer.append("\n");
        buffer.append("# Formulate the initial condition array - \n");
        buffer.append("initial_condition_array = Float64[];\n");
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");

        // Get the list of species -
        ArrayList<VLCGPBPKSpeciesModel> species_model_array = model_tree.getSpeciesModelsFromPBPKModelTree();
        int species_index = 1;
        String local_compartment_name = model_tree.getFirstCompartmentNameFromPBPKModelTree();
        Boolean write_last_comment_line = true;
        for (VLCGPBPKSpeciesModel species_model : species_model_array){

            // Get the data from the model -
            String symbol = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);
            String initial_amount = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_INITIAL_CONDITION);
            String compartment_name = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_COMPARTMENT);
            String species_type = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SPECIES_TYPE);

            if (local_compartment_name.equalsIgnoreCase(compartment_name) == false && species_type.equalsIgnoreCase("biochemical")){
                //buffer.append("# ------------------------------------------------------------------------------------------------ #\n");
                buffer.append("\n");
            }
            else if (species_type.equalsIgnoreCase("volume") && write_last_comment_line){
               //buffer.append("# ------------------------------------------------------------------------------------------------ #\n");
                buffer.append("\n");
                write_last_comment_line = false;
            }

            if (species_type.equalsIgnoreCase("biochemical")){

                // write ic record -
                buffer.append("push!(initial_condition_array,(1.0/characteristic_concentration)*");
            }
            else {
                // write ic record -
                buffer.append("push!(initial_condition_array,(1.0/characteristic_volume)*");
            }

            buffer.append(initial_amount);
            buffer.append(");\t");
            buffer.append("#\t");
            buffer.append(species_index);
            buffer.append("\t");
            buffer.append(compartment_name);
            buffer.append(" ");
            buffer.append(symbol);
            buffer.append("\n");

            local_compartment_name = compartment_name;

            // update the species count -
            species_index++;
        }
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");

        buffer.append("\n");
        buffer.append("# Formulate the time constant array - \n");
        buffer.append("time_constant_array = Float64[];\n");
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");
        species_index = 1;
        write_last_comment_line = true;
        local_compartment_name = model_tree.getFirstCompartmentNameFromPBPKModelTree();
        for (VLCGPBPKSpeciesModel species_model : species_model_array){

            // Get the data from the model -
            String symbol = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);
            String compartment_name = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_COMPARTMENT);
            String species_type = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SPECIES_TYPE);

            if (local_compartment_name.equalsIgnoreCase(compartment_name) == false && species_type.equalsIgnoreCase("biochemical")){
                //buffer.append("# ------------------------------------------------------------------------------------------------ #\n");
                buffer.append("\n");
            }
            else if (species_type.equalsIgnoreCase("volume") && write_last_comment_line){
                //buffer.append("# ------------------------------------------------------------------------------------------------ #\n");
                buffer.append("\n");
                write_last_comment_line = false;
            }

            buffer.append("push!(time_constant_array,1.0");
            buffer.append(");\t");
            buffer.append("#\t");
            buffer.append(species_index);
            buffer.append("\t");
            buffer.append(compartment_name);
            buffer.append(" ");
            buffer.append(symbol);
            buffer.append("\n");

            local_compartment_name = compartment_name;

            species_index++;
        }
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");

        // Rate constant array -
        buffer.append("\n");
        buffer.append("# Formulate the rate constant array - \n");
        buffer.append("rate_constant_array = Float64[];\n");
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");

        // Get the list of compartment models -
        ArrayList<VLCGPBPKCompartmentModel> compartment_model_array = model_tree.getCompartmentModelsFromPBPKModelTree();
        int reaction_counter = 1;
        local_compartment_name = model_tree.getFirstCompartmentNameFromPBPKModelTree();
        for (VLCGPBPKCompartmentModel compartment_model : compartment_model_array){

            // Get the symbol of the compartment -
            String compartment_symbol = (String)compartment_model.getModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL);

            buffer.append("# ");
            buffer.append(compartment_symbol);
            buffer.append("\n");

            // ok, lookup the reactions that can occur in this compartment -
            ArrayList<String> reaction_name_array = model_tree.getReactionNamesFromPBPKModelTreeInCompartmentWithSymbol(compartment_symbol);
            for (String reaction_name : reaction_name_array){

                // Get the raw string -
                String raw_string = model_tree.getRawReactionStringFromPBPKModelTreeForReactionWithNameAndCompartment(compartment_symbol,reaction_name);

                // fromulate the comment string -
                StringBuilder comment = new StringBuilder();
                comment.append(compartment_symbol);
                comment.append("\t");
                comment.append(raw_string);

                // Default value -
                float default_parameter_value = 1.0f;

                // is this a degradation reaction?
                if (model_tree.isThisADegradationReaction(reaction_name,compartment_symbol)){
                    default_parameter_value = 0.1f;

                    // write the line -
                    buffer.append("push!(rate_constant_array,(characteristic_time)*");
                    buffer.append(default_parameter_value);
                    buffer.append(");\t# ");
                    buffer.append(reaction_counter);
                    buffer.append("\t");
                    buffer.append(comment.toString());
                    buffer.append("\n");
                }
                else if (model_tree.isThisASourceReaction(reaction_name,compartment_symbol)){

                    // write the line -
                    buffer.append("push!(rate_constant_array,(characteristic_time/characteristic_concentration)*");
                    buffer.append(default_parameter_value);
                    buffer.append(");\t# ");
                    buffer.append(reaction_counter);
                    buffer.append("\t");
                    buffer.append(comment.toString());
                    buffer.append("\n");
                }
                else {

                    // write the line -
                    buffer.append("push!(rate_constant_array,(1.0/characteristic_time)*");
                    buffer.append(default_parameter_value);
                    buffer.append(");\t# ");
                    buffer.append(reaction_counter);
                    buffer.append("\t");
                    buffer.append(comment.toString());
                    buffer.append("\n");
                }

                // update the counter -
                reaction_counter++;
            }

            buffer.append("\n");
        }
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");

        buffer.append("\n");
        buffer.append("# Formulate the saturation constant array - \n");
        buffer.append("saturation_constant_array = zeros(NREACTIONS,NSPECIES);\n");
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");

        // Get the compound vector (species::compartment) - we use this for ordering
        ArrayList<String> lookup_species_order_vector = model_tree.makeSpeciesCompartmentSymbolArrayForPBPKModelTree();
        reaction_counter = 1;
        for (VLCGPBPKCompartmentModel compartment_model : compartment_model_array) {

            // Get the symbol of the compartment -
            String compartment_symbol = (String) compartment_model.getModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL);

            ArrayList<String> reaction_name_array = model_tree.getReactionNamesFromPBPKModelTreeInCompartmentWithSymbol(compartment_symbol);
            for (String reaction_name : reaction_name_array) {

                // Get the raw string -
                String raw_reaction_string = model_tree.getRawReactionStringFromPBPKModelTreeForReactionWithNameAndCompartment(compartment_symbol, reaction_name);

                // ok, if we have a source -or- degradation reaction, these are 0 and 1st order => no sat coefficient
                if (model_tree.isThisADegradationReaction(reaction_name,compartment_symbol) == false &&
                        model_tree.isThisASourceReaction(reaction_name,compartment_symbol) == false){

                    // for this reaction, in this compartment I need to get the index of the species?
                    ArrayList<VLCGPBPKSpeciesModel> reactant_array = model_tree.getReactantsForBiochemicalReactionWithNameInCompartment(reaction_name,compartment_symbol);
                    for (VLCGPBPKSpeciesModel local_species_model : reactant_array){

                        // create the compound symbol -
                        String local_reactant_symbol = (String)local_species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);
                        String local_compound_name = local_reactant_symbol+"::"+compartment_symbol;

                        // what is the index of local_compound_name?
                        int local_species_index = lookup_species_order_vector.indexOf(local_compound_name) + 1;

                        // write the line ...
                        buffer.append("saturation_constant_array[");
                        buffer.append(reaction_counter);
                        buffer.append(",");
                        buffer.append(local_species_index);
                        buffer.append("] = 1.0;\n");
                    }
                }

                // update the reaction counter -
                reaction_counter++;
            }
        }
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");

        // Control parameter array -
        int number_of_control_terms = model_tree.calculateTheTotalNumberOfControlTerms();
        buffer.append("\n");
        buffer.append("# Formulate control parameter array - \n");
        buffer.append("control_parameter_array = zeros(");
        buffer.append(number_of_control_terms);
        buffer.append(",2);\n");
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");

        int control_counter = 1;
        for (VLCGPBPKCompartmentModel compartment_model : compartment_model_array) {

            // Get the symbol of the compartment -
            String compartment_symbol = (String) compartment_model.getModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL);

            buffer.append("# ");
            buffer.append(compartment_symbol);
            buffer.append("\n");

            // what are the control terms for this compartment -
            ArrayList<VLCGPBPKBiochemistryControlModel> control_model_array = model_tree.getBiochemistryControlModelFromPBPKModelTreeForCompartmentWithSymbol(compartment_symbol);
            for (VLCGPBPKBiochemistryControlModel control_model : control_model_array){

                // Get the control comment string -
                String comment_string = (String)control_model.getModelComponent(VLCGPBPKBiochemistryControlModel.FORMATTED_RAW_RECORD);

                // write the record -
                buffer.append("control_parameter_array[");
                buffer.append(control_counter);
                buffer.append(",1] = 0.1;\t#\t");
                buffer.append(control_counter);
                buffer.append(" Gain: \t");
                buffer.append(comment_string);
                buffer.append("\n");

                // update the control counter -
                //control_counter++;

                // write the order line -
                buffer.append("control_parameter_array[");
                buffer.append(control_counter);
                buffer.append(",2] = 1.0;\t#\t");
                buffer.append(control_counter);
                buffer.append(" Order: \t");
                buffer.append(comment_string);
                buffer.append("\n");

                // update the control counter -
                control_counter++;
            }

            buffer.append("\n");
        }
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");

        // For each inout flow, we need to setup the mass
        // of biochemical species carried into the system -
        buffer.append("\n");
        buffer.append("# Input concentration array - \n");
        buffer.append("input_concentration_array = Float64[]\n");
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");
        species_model_array = model_tree.getSpeciesModelsFromPBPKModelTree();
        int input_concentration_index = 1;
        for (VLCGPBPKSpeciesModel species_model : species_model_array) {

            String species_symbol = (String) species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);

            // ok, we the species -
            String species_type = (String) species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SPECIES_TYPE);
            if (species_type.equalsIgnoreCase("volume") == false) {

                String home_compartment_symbol = (String) species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_COMPARTMENT);
                ArrayList<String> upstream_compartment_symbol_array = model_tree.getListOfCompartmentsUpstreamOfCompartmentWithSymbol(home_compartment_symbol);
                for (String upstream_compartment : upstream_compartment_symbol_array) {

                    // ok, if we have an upstream component that is [] -> we have a souce term
                    if (upstream_compartment.equalsIgnoreCase("[]")) {

                        // ok, generate an input species by default -
                        String special_symbol = species_symbol + "_input_" + home_compartment_symbol;
                        buffer.append("push!(input_concentration_array,(1.0/characteristic_concentration)*0.0);\t# ");
                        buffer.append(input_concentration_index++);
                        buffer.append(" ");
                        buffer.append(special_symbol);
                        buffer.append("\n");
                    }
                }
            }
        }
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");


        buffer.append("\n");
        buffer.append("# ---------------------------- DO NOT EDIT BELOW THIS LINE --------------------------------------- #\n");
        buffer.append("data_dictionary = Dict();\n");
        buffer.append("data_dictionary[\"CHARACTERISTIC_VARIABLE_ARRAY\"] = characteristic_variable_array;\n");
        buffer.append("data_dictionary[\"INPUT_CONCENTRATION_ARRAY\"] = input_concentration_array;\n");
        buffer.append("data_dictionary[\"DEFAULT_BEATS_PER_MINUTE\"] = default_beats_per_minute;\n");
        buffer.append("data_dictionary[\"DEFAULT_STROKE_VOLUME\"] = default_stroke_volume;\n");
        buffer.append("data_dictionary[\"FLOW_PARAMETER_ARRAY\"] = flow_parameter_array;\n");
        buffer.append("data_dictionary[\"STOICHIOMETRIC_MATRIX\"] = S;\n");
        buffer.append("data_dictionary[\"FLOW_CONNECTIVITY_MATRIX\"] = C;\n");
        buffer.append("data_dictionary[\"RATE_CONSTANT_ARRAY\"] = rate_constant_array;\n");
        buffer.append("data_dictionary[\"SATURATION_CONSTANT_ARRAY\"] = saturation_constant_array;\n");
        buffer.append("data_dictionary[\"INITIAL_CONDITION_ARRAY\"] = initial_condition_array;\n");
        buffer.append("data_dictionary[\"TIME_CONSTANT_ARRAY\"] = time_constant_array;\n");
        buffer.append("data_dictionary[\"CONTROL_PARAMETER_ARRAY\"] = control_parameter_array;\n");
        buffer.append("# ------------------------------------------------------------------------------------------------ #\n");

        // last line -
        buffer.append("return data_dictionary;\n");
        buffer.append("end\n");

        // return the buffer -
        return buffer.toString();
    }

    public String buildDriverFunctionBuffer(VLCGPBPKModelTreeWrapper model_tree, VLCGTransformationPropertyTree property_tree) throws Exception {

        // String buffer -
        StringBuffer driver = new StringBuffer();

        // We need to get the imports -
        String balance_filename = property_tree.lookupKwateeBalanceFunctionName()+".jl";
        driver.append("include(\"");
        driver.append(balance_filename);
        driver.append("\")\n");
        driver.append("using Sundials;\n");
        driver.append("\n");

        // Copyright notice -
        String copyright = copyrightFactory.getJuliaCopyrightHeader();
        driver.append(copyright);

        // Get the function name -
        String function_name = property_tree.lookupKwateeDriverFunctionName();
        driver.append("function ");
        driver.append(function_name);
        driver.append("(TSTART,TSTOP,Ts,data_dictionary)\n");

        driver.append("# ----------------------------------------------------------------------------------- #\n");
        driver.append("# ");
        driver.append(function_name);
        driver.append(".jl was generated using the Kwatee code generation system.\n");
        driver.append("# ");
        driver.append(function_name);
        driver.append(": Solves model equations from TSTART to TSTOP given parameters in data_dictionary.\n");
        driver.append("# Username: ");
        driver.append(property_tree.lookupKwateeModelUsername());
        driver.append("\n");
        driver.append("# Type: ");
        driver.append(property_tree.lookupKwateeModelType());
        driver.append("\n");
        driver.append("# Version: ");
        driver.append(property_tree.lookupKwateeModelVersion());
        driver.append("\n");
        driver.append("# Generation timestamp: ");
        driver.append(date_formatter.format(today));
        driver.append("\n");
        driver.append("# \n");
        driver.append("# Input arguments: \n");
        driver.append("# TSTART  - Time start \n");
        driver.append("# TSTOP  - Time stop \n");
        driver.append("# Ts - Time step \n");
        driver.append("# data_dictionary  - Data dictionary instance (holds model parameters) \n");
        driver.append("# \n");
        driver.append("# Return arguments: \n");
        driver.append("# TSIM - Simulation time vector \n");
        driver.append("# X - Simulation state array (NTIME x NSPECIES) \n");
        driver.append("# ----------------------------------------------------------------------------------- #\n");
        driver.append("\n");

        driver.append("# Get required stuff from DataFile struct -\n");
        driver.append("TSIM = [TSTART:Ts:TSTOP];\n");
        driver.append("initial_condition_vector = data_dictionary[\"INITIAL_CONDITION_ARRAY\"];\n");
        driver.append("\n");

        driver.append("# Call the ODE solver - \n");
        driver.append("fbalances(t,y,ydot) = ");
        driver.append(property_tree.lookupKwateeBalanceFunctionName());
        driver.append("(t,y,ydot,data_dictionary);\n");
        driver.append("X = Sundials.cvode(fbalances,initial_condition_vector,TSIM);\n");
        driver.append("\n");
        driver.append("return (TSIM,X);\n");

        // last line -
        driver.append("end\n");

        // return the populated buffer -
        return driver.toString();
    }
}
