package org.varnerlab.kwatee.pbpkmodel;

import org.varnerlab.kwatee.foundation.VLCGCopyrightFactory;
import org.varnerlab.kwatee.foundation.VLCGTransformationPropertyTree;
import org.varnerlab.kwatee.pbpkmodel.model.VLCGPBPKBiochemistryControlModel;
import org.varnerlab.kwatee.pbpkmodel.model.VLCGPBPKCompartmentModel;
import org.varnerlab.kwatee.pbpkmodel.model.VLCGPBPKSpeciesModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

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


    public String buildBalanceFunctionBuffer(VLCGPBPKModelTreeWrapper model_tree, VLCGTransformationPropertyTree property_tree) throws Exception {

        // Method variables -
        StringBuffer massbalances = new StringBuffer();

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
        massbalances.append("# Correct nagative x's = throws errors in control even if small - \n");
        massbalances.append("idx = find(x->(x<0),x);\n");
        massbalances.append("x[idx] = 0.0;\n");
        massbalances.append("\n");
        massbalances.append("# Call the kinetics function - \n");
        massbalances.append("(rate_vector) = ");
        massbalances.append(kinetics_function_name);
        massbalances.append("(t,x,data_dictionary);\n");

        massbalances.append("\n");
        massbalances.append("# Call the control function - \n");
        massbalances.append("(rate_vector) = ");
        massbalances.append(control_function_name);
        massbalances.append("(t,x,rate_vector,data_dictionary);\n");
        massbalances.append("\n");

        // check - is this model large scale optimized?
        if (property_tree.isKwateeModelLargeScaleOptimized() == true){

            // build explicit list of balance equations -

        }
        else {

            // balance are encoded as matrix vector product -
            massbalances.append("# Encode the balance equations as a matrix vector product - \n");
            massbalances.append("maximum_specific_growth_rate = data_dictionary[\"MAXIMUM_SPECIFIC_GROWTH_RATE\"];\n");
            massbalances.append("S = data_dictionary[\"STOICHIOMETRIC_MATRIX\"];\n");
            massbalances.append("dilution_selection_matrix = data_dictionary[\"DILUTION_SELECTION_MATRIX\"];\n");
            massbalances.append("tau_array = data_dictionary[\"TIME_CONSTANT_ARRAY\"];\n");
            massbalances.append("tmp_vector = S*rate_vector;\n");
            massbalances.append("number_of_states = length(tmp_vector);\n");
            massbalances.append("for state_index in [1:number_of_states]\n");
            massbalances.append("\tdxdt_vector[state_index] = tmp_vector[state_index] - maximum_specific_growth_rate*(dilution_selection_matrix[state_index,state_index])*(x[state_index]);\n");
            massbalances.append("\tdxdt_vector[state_index] = tau_array[state_index]*dxdt_vector[state_index];\n");
            massbalances.append("end");
            massbalances.append("\n");
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

        // write out the species list -
        ArrayList<VLCGPBPKSpeciesModel> species_model_array = model_tree.getSpeciesModelsFromPBPKModelTree();
        int species_index = 1;
        for (VLCGPBPKSpeciesModel species_model : species_model_array){

            // Get data from the model -
            String symbol = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);
            String compartment_name = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_COMPARTMENT);

            // write the line -
            buffer.append(symbol);
            buffer.append("_");
            buffer.append(compartment_name);
            buffer.append(" = ");
            buffer.append("x[");
            buffer.append(species_index);
            buffer.append("];\n");

            // update the species index -
            species_index++;
        }

        buffer.append("\n");
        buffer.append("# ----------------------------------------------------------------------------------- #\n");

        // Write transfer function
        ArrayList<VLCGPBPKCompartmentModel> compartment_model_array = model_tree.getCompartmentModelsFromPBPKModelTree();
        int control_index = 1;
        int reaction_index = 1;
        for (VLCGPBPKCompartmentModel compartment_model : compartment_model_array) {

            // Get the symbol of the compartment -
            String compartment_symbol = (String) compartment_model.getModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL);

            // what are the control terms for this compartment -
            ArrayList<VLCGPBPKBiochemistryControlModel> control_model_array = model_tree.getBiochemistryControlModelFromPBPKModelTreeForCompartmentWithSymbol(compartment_symbol);
            for (VLCGPBPKBiochemistryControlModel control_model : control_model_array) {

                // Get the control comment string -
                String comment_string = (String) control_model.getModelComponent(VLCGPBPKBiochemistryControlModel.FORMATTED_RAW_RECORD);
                String raw_actor = (String)control_model.getModelComponent(VLCGPBPKBiochemistryControlModel.BIOCHEMISTRY_CONTROL_ACTOR);
                String target = (String)control_model.getModelComponent(VLCGPBPKBiochemistryControlModel.BIOCHEMISTRY_CONTROL_TARGET);
                String type = (String)control_model.getModelComponent(VLCGPBPKBiochemistryControlModel.BIOCHEMISTRY_CONTROL_TYPE);

                String actor = raw_actor+"_"+compartment_symbol;

                reaction_index = model_tree.findIndexForReactionWithNameInCompartment(target,compartment_symbol);

                // Check the type -
                buffer.append("# ");
                buffer.append(comment_string);
                buffer.append("\n");
                if (type.equalsIgnoreCase("repression") || type.equalsIgnoreCase("inhibition")){

                    // check do we have a zero inhibitor?
                    buffer.append("if (");
                    buffer.append(actor);
                    buffer.append("<EPSILON);\n");
                    buffer.append("\tpush!(transfer_function_vector,0.0);\n");
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
                }

                // update -
                control_index++;
            }

            // integrate the transfer functions -
            buffer.append("control_vector[");
            buffer.append(reaction_index);
            buffer.append("] = mean(transfer_function_vector);\n");
            buffer.append("transfer_function_vector = 0;\n");
            buffer.append("# ----------------------------------------------------------------------------------- #\n");
            buffer.append("\n");
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

        // write out the species list -
        ArrayList<VLCGPBPKSpeciesModel> species_model_array = model_tree.getSpeciesModelsFromPBPKModelTree();
        int species_index = 1;
        for (VLCGPBPKSpeciesModel species_model : species_model_array){

            // Get data from the model -
            String symbol = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);
            String compartment_name = (String)species_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_COMPARTMENT);

            // write the line -
            buffer.append(symbol);
            buffer.append("_");
            buffer.append(compartment_name);
            buffer.append(" = ");
            buffer.append("x[");
            buffer.append(species_index);
            buffer.append("];\n");

            // update the species index -
            species_index++;
        }

        // Write out the kinetics vector -
        buffer.append("\n");
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

                        buffer.append("*");
                        buffer.append(species_symbol);
                        buffer.append(";\n");
                    }
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
        buffer.append("# Load the stoichiometric matrix - \n");

        // Get the path to the stoichiometric matrix -
        String fully_qualified_stoichiometric_matrix_path = property_tree.lookupKwateeStoichiometricMatrixFilePath();
        buffer.append("S = float(open(readdlm,");
        buffer.append("\"");
        buffer.append(fully_qualified_stoichiometric_matrix_path);
        buffer.append("\"));\n");
        buffer.append("(NSPECIES,NREACTIONS) = size(S);\n");

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
                buffer.append("# ------------------------------------------------------------------------------------------------ #\n");
            }
            else if (species_type.equalsIgnoreCase("volume") && write_last_comment_line){
                buffer.append("# ------------------------------------------------------------------------------------------------ #\n");
                write_last_comment_line = false;
            }

            // write ic record -
            buffer.append("push!(initial_condition_array,");
            buffer.append(initial_amount);
            buffer.append(");\t");
            buffer.append("#\t");
            buffer.append(species_index);
            buffer.append("\t");
            buffer.append(symbol);
            buffer.append("::");
            buffer.append(compartment_name);
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
                buffer.append("# ------------------------------------------------------------------------------------------------ #\n");
            }
            else if (species_type.equalsIgnoreCase("volume") && write_last_comment_line){
                buffer.append("# ------------------------------------------------------------------------------------------------ #\n");
                write_last_comment_line = false;
            }

            buffer.append("push!(time_constant_array,1.0");
            buffer.append(");\t");
            buffer.append("#\t");
            buffer.append(species_index);
            buffer.append("\t");
            buffer.append(symbol);
            buffer.append("::");
            buffer.append(compartment_name);
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
        for (VLCGPBPKCompartmentModel compartment_model : compartment_model_array){

            // Get the symbol of the compartment -
            String compartment_symbol = (String)compartment_model.getModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL);

            // ok, lookup the reactions that can occur in this compartment -
            ArrayList<String> reaction_name_array = model_tree.getReactionNamesFromPBPKModelTreeInCompartmentWithSymbol(compartment_symbol);
            for (String reaction_name : reaction_name_array){

                // Get the raw string -
                String raw_string = model_tree.getRawReactionStringFromPBPKModelTreeForReactionWithNameAndCompartment(compartment_symbol,reaction_name);

                // Default value -
                float default_parameter_value = 1.0f;

                // is this a degradation reaction?
                if (model_tree.isThisADegradationReaction(reaction_name,compartment_symbol)){
                    default_parameter_value = 0.1f;
                }

                // fromulate the comment string -
                StringBuilder comment = new StringBuilder();
                comment.append(raw_string);
                comment.append("\t");
                comment.append(compartment_symbol);

                // write the line -
                buffer.append("push!(rate_constant_array,");
                buffer.append(default_parameter_value);
                buffer.append(");\t# ");
                buffer.append(reaction_counter);
                buffer.append("\t");
                buffer.append(comment.toString());
                buffer.append("\n");

                // update the counter -
                reaction_counter++;
            }

            buffer.append("# ------------------------------------------------------------------------------------------------ #\n");
        }

        buffer.append("\n");
        buffer.append("# Formulate the saturation constant array - \n");
        buffer.append("saturation_constant_array = zeros(NREACTIONS,NSPECIES);\n");

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

        // Control parameter array -
        int number_of_control_terms = model_tree.calculateTheTotalNumberOfControlTerms();
        buffer.append("\n");
        buffer.append("# Formulate control parameter array - \n");
        buffer.append("control_parameter_array = zeros(");
        buffer.append(number_of_control_terms);
        buffer.append(",2);\n");

        int control_counter = 1;
        for (VLCGPBPKCompartmentModel compartment_model : compartment_model_array) {

            // Get the symbol of the compartment -
            String compartment_symbol = (String) compartment_model.getModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL);

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

                // write the order line -
                buffer.append("control_parameter_array[");
                buffer.append(control_counter);
                buffer.append(",2] = 1.0;\t#\t");
                buffer.append(control_counter);
                buffer.append(" Order: \t");
                buffer.append(comment_string);
                buffer.append("\n\n");

                // update the control counter -
                control_counter++;
            }
        }



        buffer.append("\n");
        buffer.append("# ---------------------------- DO NOT EDIT BELOW THIS LINE --------------------------------------- #\n");
        buffer.append("data_dictionary = Dict();\n");
        buffer.append("data_dictionary[\"STOICHIOMETRIC_MATRIX\"] = S;\n");
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
