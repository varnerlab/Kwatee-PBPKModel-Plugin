package org.varnerlab.kwatee.pbpkmodel;

import org.varnerlab.kwatee.foundation.VLCGInputHandler;
import org.varnerlab.kwatee.foundation.VLCGTransformationPropertyTree;
import org.varnerlab.kwatee.pbpkmodel.model.*;
import org.varnerlab.kwatee.pbpkmodel.parserdelegate.VLCGParserHandlerDelegate;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

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
public class VLCGParseVarnerPBPKFlatFile implements VLCGInputHandler {

    // instance variables -
    private VLCGTransformationPropertyTree _transformation_properties_tree = null;
    private final String _package_name_parser_delegate = "org.varnerlab.kwatee.pbpkmodel.parserdelegate";
    private Hashtable<Class,Vector<VLCGPBPKModelComponent>> _model_component_table = new Hashtable();
    private Vector<String> _species_vector = new Vector<String>();
    private Vector<VLCGPBPKCompartmentModel> _compartment_model_vector = new Vector<VLCGPBPKCompartmentModel>();
    private Vector<VLCGPBPKSpeciesModel> _species_model_vector = new Vector<VLCGPBPKSpeciesModel>();


    @Override
    public void setPropertiesTree(VLCGTransformationPropertyTree properties_tree) {

        if (properties_tree == null){
            return;
        }

        _transformation_properties_tree = properties_tree;
    }

    @Override
    public void loadResource(Object o) throws Exception {

        // Where is the file that I need to load?
        String resource_file_path = _transformation_properties_tree.lookupKwateeNetworkFilePath();
        if (resource_file_path != null){

            // ok, we have what appears to be a path, read the PBPK file at this location -
            // this will populate the _model_component_table -
            _readPBPKFlatFile(resource_file_path);
        }
        else {
            throw new Exception("ERROR: Missing resource file path. Can't find GRN description to parse.");
        }
    }

    @Override
    public Object getResource(Object o) throws Exception {

        // Method variables -
        StringBuilder xml_buffer = new StringBuilder();
        DocumentBuilder document_builder = null;
        Document model_tree = null;

        // Generate sections and add them to the tree -
        xml_buffer.append("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>\n");
        xml_buffer.append("<PBPKModel>\n");

        // List of compartments -
        String compartment_tags = _generateListOfCompartments();
        xml_buffer.append("\t<listOfCompartments>\n");
        xml_buffer.append(compartment_tags);
        xml_buffer.append("\t</listOfCompartments>\n");
        xml_buffer.append("\n");

        // List of compartment connections -
        String compartment_connections = _generateListOfCompartmentConnections();
        xml_buffer.append("\t<listOfCompartmentConnections>\n");
        xml_buffer.append(compartment_connections);
        xml_buffer.append("\t</listOfCompartmentConnections>\n");
        xml_buffer.append("\n");

        // List of species -
        String global_species_list = _generateListOfBiochemicalSpecies();
        xml_buffer.append("\t<listOfSpecies>\n");
        xml_buffer.append(global_species_list);
        xml_buffer.append("\t</listOfSpecies>\n");
        xml_buffer.append("\n");

        // List of reactions -
        String global_list_of_reactions = _generateListOfBiochemicalReactions();
        xml_buffer.append("\t<listOfReactions>\n");
        xml_buffer.append(global_list_of_reactions);
        xml_buffer.append("\t</listOfReactions>\n");
        xml_buffer.append("\n");

        // List of control -
        String global_list_of_biochemical_control_terms = _generateListOfBiochemicalControlTerms();
        xml_buffer.append("\t<listOfBiochemistryControlTerms>\n");
        xml_buffer.append(global_list_of_biochemical_control_terms);
        xml_buffer.append("\t</listOfBiochemistryControlTerms>\n");
        xml_buffer.append("\n");
        
        // Flow rules -
        String global_list_of_species_rules = _generateListOfSpeciesFlowRules();
        xml_buffer.append("\t<listOfSpeciesRules>\n");
        xml_buffer.append(global_list_of_species_rules);
        xml_buffer.append("\t</listOfSpeciesRules>\n");
        xml_buffer.append("\n");

        xml_buffer.append("</PBPKModel>\n");

        // Convert the string buffer into an XML Document object -
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        document_builder = factory.newDocumentBuilder();
        model_tree = document_builder.parse(new InputSource(new StringReader(xml_buffer.toString())));

        // write the tree to the debug folder -
        // Get the debug path -
        String debug_path = _transformation_properties_tree.lookupKwateeDebugPath();
        if (debug_path != null){

            // ok, we have a path - is this path legit?
            File oFile = new File(debug_path);
            if (oFile.isDirectory()){

                // Create new path -
                String fully_qualified_model_path = debug_path+"PBPK_AST.xml";

                // Write the AST file -
                File ast_file = new File(fully_qualified_model_path);
                BufferedWriter writer = new BufferedWriter(new FileWriter(ast_file));

                // Write buffer to file system and close writer
                writer.write(xml_buffer.toString());
                writer.close();
            }
        }

        // return the wrapped model_tree -
        VLCGPBPKModelTreeWrapper model_wrapper = new VLCGPBPKModelTreeWrapper(model_tree);
        return model_wrapper;
    }


    // private helper methods -
    private String _generateListOfSpeciesFlowRules() throws Exception {
    
        // Method variables -
        StringBuilder buffer = new StringBuilder();

        String class_name_key = _package_name_parser_delegate + ".VLCGPBPKSpeciesRulesParserDelegate";
        Vector<VLCGPBPKSpeciesModel> species_model_vector = (Vector)_model_component_table.get(Class.forName(class_name_key));
        for (VLCGPBPKSpeciesModel model : species_model_vector){

            // Get data from the model -
            String species_symbol = (String)model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);
            String rule_type = (String)model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_RULE_TYPE);

            // write the record -
            buffer.append("\t\t");
            buffer.append("<speciesRule species=\"");
            buffer.append(species_symbol);
            buffer.append("\" type=\"");
            buffer.append(rule_type);
            buffer.append("\"");

            // does this model contain a rule_compartemnt?
            if (model.containsKey(VLCGPBPKSpeciesModel.SPECIES_RULE_COMPARTMENT)){

                String rule_compartment = (String)model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_RULE_COMPARTMENT);

                buffer.append(" compartment=\"");
                buffer.append(rule_compartment);
                buffer.append("\"");
            }

            // write closing line -
            buffer.append("/>\n");
        }

        // return the buffer -
        return buffer.toString();
    }
    
    private String _generateListOfBiochemicalControlTerms() throws Exception {

        // Method variables -
        StringBuilder buffer = new StringBuilder();

        // Get the control -
        String class_name_key = _package_name_parser_delegate + ".VLCGPBPKCompartmentBiochemistryControlParserDelegate";
        Vector<VLCGPBPKBiochemistryControlModel> control_vector = (Vector)_model_component_table.get(Class.forName(class_name_key));
        Iterator<VLCGPBPKBiochemistryControlModel> control_vector_iterator = control_vector.iterator();
        while (control_vector_iterator.hasNext()){

            // Get the model -
            VLCGPBPKBiochemistryControlModel control_model = control_vector_iterator.next();

            // Get data from the model -
            String control_name = (String)control_model.getModelComponent(VLCGPBPKBiochemistryControlModel.BIOCHEMISTRY_CONTROL_NAME);
            String control_actor = (String)control_model.getModelComponent(VLCGPBPKBiochemistryControlModel.BIOCHEMISTRY_CONTROL_ACTOR);
            String control_target = (String)control_model.getModelComponent(VLCGPBPKBiochemistryControlModel.BIOCHEMISTRY_CONTROL_TARGET);
            String control_type = (String)control_model.getModelComponent(VLCGPBPKBiochemistryControlModel.BIOCHEMISTRY_CONTROL_TYPE);
            String control_compartment = (String)control_model.getModelComponent(VLCGPBPKBiochemistryControlModel.BIOCHEMISTRY_CONTROL_COMPARTMENT);


            // ok, do we have a *?
            if (control_compartment.equalsIgnoreCase("*")){

                for (VLCGPBPKCompartmentModel compartment_model : _compartment_model_vector){

                    // Get the compartment symbol -
                    String compartment_symbol = (String) compartment_model.getModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL);

                    buffer.append("\t\t");
                    buffer.append("<control name=\"");
                    buffer.append(control_name);
                    buffer.append("_");
                    buffer.append(compartment_symbol);
                    buffer.append("\" actor=\"");
                    buffer.append(control_actor);
                    buffer.append("\" target=\"");
                    buffer.append(control_target);
                    buffer.append("\" type=\"");
                    buffer.append(control_type);
                    buffer.append("\" compartment=\"");
                    buffer.append(compartment_symbol);
                    buffer.append("\"/>\n");
                }
            }
            else {

                // we have a specific compartment - build the record
                buffer.append("\t\t");
                buffer.append("<control name=\"");
                buffer.append(control_name);
                buffer.append("_");
                buffer.append(control_compartment);
                buffer.append("\" actor=\"");
                buffer.append(control_actor);
                buffer.append("\" target=\"");
                buffer.append(control_target);
                buffer.append("\" type=\"");
                buffer.append(control_type);
                buffer.append("\" compartment=\"");
                buffer.append(control_compartment);
                buffer.append("\"/>\n");
            }
        }


        // return the populated buffer -
        return buffer.toString();
    }


    private String _generateListOfBiochemicalReactions() throws Exception {

        // Method variables -
        StringBuilder buffer = new StringBuilder();

        // Get the translation reactions -
        String class_name_key = _package_name_parser_delegate + ".VLCGPBPKCompartmentBiochemistryParserDelegate";
        Vector<VLCGPBPKBiochemistryReactionModel> reaction_vector = (Vector)_model_component_table.get(Class.forName(class_name_key));
        ListIterator<VLCGPBPKBiochemistryReactionModel> reaction_iterator = reaction_vector.listIterator();
        Vector<VLCGPBPKBiochemistryReactionModel> tmp_vector_reaction = new Vector<VLCGPBPKBiochemistryReactionModel>();
        Vector<VLCGPBPKBiochemistryReactionModel> complete_reaction_vector = new Vector<VLCGPBPKBiochemistryReactionModel>();
        while (reaction_iterator.hasNext()) {

            // get the connection model -
            VLCGPBPKBiochemistryReactionModel reaction_model = (VLCGPBPKBiochemistryReactionModel)reaction_iterator.next();
            reaction_model.doExecute();

            // where is this reaction taking place?
            String reaction_location = (String)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_COMPARTMENT_SYMBOL);
            if (reaction_location.equalsIgnoreCase("*")){

                // ok, this reaction takes place in *all* compartments -
                // replace this reaction object, with an exact copy in all the compartments -
                //reaction_iterator.remove();

                int number_of_compartments = _compartment_model_vector.size();
                for (int compartment_index = 0;compartment_index<number_of_compartments;compartment_index++){

                    // get compartment model (and symbol) -
                    VLCGPBPKCompartmentModel compartment_model = _compartment_model_vector.get(compartment_index);
                    String compartment_symbol = (String)compartment_model.getModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL);
                    String formatted_raw_string = (String)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.FORMATTED_RAW_RECORD);
                    String reaction_name = (String)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_NAME);
                    String reverse_flag = (String)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_REVERSE);
                    String forward_flag = (String)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_FORWARD);
                    String enzyme_symbol = (String)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_ENZYME_SYMBOL);
                    Vector<VLCGPBPKSpeciesModel> reactant_vector = (Vector)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.BIOCHEMISTRY_REACTION_REACTANT_VECTOR);
                    Vector<VLCGPBPKSpeciesModel> product_vector = (Vector)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.BIOCHEMISTRY_REACTION_PRODUCT_VECTOR);


                    // update the reaction model -
                    VLCGPBPKBiochemistryReactionModel reaction_model_copy = new VLCGPBPKBiochemistryReactionModel();
                    reaction_model_copy.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_COMPARTMENT_SYMBOL,compartment_symbol);
                    reaction_model_copy.setModelComponent(VLCGPBPKBiochemistryReactionModel.BIOCHEMISTRY_REACTION_PRODUCT_VECTOR,product_vector);
                    reaction_model_copy.setModelComponent(VLCGPBPKBiochemistryReactionModel.BIOCHEMISTRY_REACTION_REACTANT_VECTOR,reactant_vector);
                    reaction_model_copy.setModelComponent(VLCGPBPKBiochemistryReactionModel.FORMATTED_RAW_RECORD,formatted_raw_string);
                    reaction_model_copy.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_NAME,reaction_name);
                    reaction_model_copy.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_REVERSE,reverse_flag);
                    reaction_model_copy.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_FORWARD,forward_flag);
                    reaction_model_copy.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_ENZYME_SYMBOL,enzyme_symbol);
                    tmp_vector_reaction.addElement(reaction_model_copy);
                }
            }
            else {
                tmp_vector_reaction.addElement(reaction_model);
            }
        }

        // do we have any -inf,inf reactions?
        Iterator<VLCGPBPKBiochemistryReactionModel> tmp_reaction_iterator = tmp_vector_reaction.iterator();
        while (tmp_reaction_iterator.hasNext()){

            // get the reaction -
            VLCGPBPKBiochemistryReactionModel reaction_model = tmp_reaction_iterator.next();
            String reverse_flag = (String)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_REVERSE);

            if (reverse_flag.equalsIgnoreCase("0") == false){

                // we have a reverse reaction ...
                String compartment_symbol = (String)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_COMPARTMENT_SYMBOL);
                String formatted_raw_string = (String)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.FORMATTED_RAW_RECORD);
                String reaction_name = (String)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_NAME);
                String forward_flag = (String)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_FORWARD);
                String enzyme_symbol = (String)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_ENZYME_SYMBOL);
                Vector<VLCGPBPKSpeciesModel> reactant_vector = (Vector)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.BIOCHEMISTRY_REACTION_REACTANT_VECTOR);
                Vector<VLCGPBPKSpeciesModel> product_vector = (Vector)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.BIOCHEMISTRY_REACTION_PRODUCT_VECTOR);

                // update the reaction model -
                VLCGPBPKBiochemistryReactionModel reaction_model_copy_forward = new VLCGPBPKBiochemistryReactionModel();
                reaction_model_copy_forward.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_COMPARTMENT_SYMBOL,compartment_symbol);
                reaction_model_copy_forward.setModelComponent(VLCGPBPKBiochemistryReactionModel.BIOCHEMISTRY_REACTION_PRODUCT_VECTOR,product_vector);
                reaction_model_copy_forward.setModelComponent(VLCGPBPKBiochemistryReactionModel.BIOCHEMISTRY_REACTION_REACTANT_VECTOR,reactant_vector);
                reaction_model_copy_forward.setModelComponent(VLCGPBPKBiochemistryReactionModel.FORMATTED_RAW_RECORD,formatted_raw_string);
                reaction_model_copy_forward.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_NAME,reaction_name);
                reaction_model_copy_forward.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_REVERSE,"0.0");
                reaction_model_copy_forward.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_FORWARD,forward_flag);
                reaction_model_copy_forward.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_ENZYME_SYMBOL,enzyme_symbol);
                complete_reaction_vector.addElement(reaction_model_copy_forward);

                // update the reaction model -
                VLCGPBPKBiochemistryReactionModel reaction_model_copy_reverse = new VLCGPBPKBiochemistryReactionModel();
                reaction_model_copy_reverse.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_COMPARTMENT_SYMBOL,compartment_symbol);
                reaction_model_copy_reverse.setModelComponent(VLCGPBPKBiochemistryReactionModel.BIOCHEMISTRY_REACTION_PRODUCT_VECTOR,reactant_vector);
                reaction_model_copy_reverse.setModelComponent(VLCGPBPKBiochemistryReactionModel.BIOCHEMISTRY_REACTION_REACTANT_VECTOR,product_vector);
                reaction_model_copy_reverse.setModelComponent(VLCGPBPKBiochemistryReactionModel.FORMATTED_RAW_RECORD,formatted_raw_string);
                reaction_model_copy_reverse.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_NAME,reaction_name+"_reverse");
                reaction_model_copy_reverse.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_REVERSE,"0.0");
                reaction_model_copy_reverse.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_FORWARD,forward_flag);
                reaction_model_copy_reverse.setModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_ENZYME_SYMBOL,enzyme_symbol);
                complete_reaction_vector.addElement(reaction_model_copy_reverse);
            }
            else {
                complete_reaction_vector.addElement(reaction_model);
            }
        }

        // iterate the updated reaction vector again, organized by compartment -
        int reaction_index = 1;
        int number_of_compartments = _compartment_model_vector.size();
        for (int compartment_index = 0;compartment_index<number_of_compartments;compartment_index++) {

            // get compartment model (and symbol) -
            VLCGPBPKCompartmentModel compartment_model = _compartment_model_vector.get(compartment_index);
            String compartment_symbol = (String) compartment_model.getModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL);

            // compartment wrapper -
            buffer.append("\t\t");
            buffer.append("<location compartment=\"");
            buffer.append(compartment_symbol);
            buffer.append("\">\n");

            // Get reactions in this compartment -
            Vector<VLCGPBPKBiochemistryReactionModel> sorted_reaction_vector = _findReactionsInCompartmentWithSymbol(compartment_symbol,complete_reaction_vector);
            Iterator<VLCGPBPKBiochemistryReactionModel> sorted_reaction_iterator = sorted_reaction_vector.iterator();
            while (sorted_reaction_iterator.hasNext()){

                // Get the model -
                VLCGPBPKBiochemistryReactionModel local_reaction_model = sorted_reaction_iterator.next();

                // Get data from the model -
                String reaction_name = (String)local_reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_NAME);
                String formatted_reaction_string = (String)local_reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.FORMATTED_RAW_RECORD);
                String enzyme_symbol = (String)local_reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_ENZYME_SYMBOL);

                buffer.append("\t\t\t");
                buffer.append("<reaction name=\"");
                buffer.append(reaction_name);
                buffer.append("\" enzyme_symbol=\"");
                buffer.append(enzyme_symbol);
                buffer.append("\" formatted_raw_string=\"");
                buffer.append(formatted_reaction_string);
                buffer.append("\" index=\"");
                buffer.append(reaction_index);
                buffer.append("\">\n");
                buffer.append("\t\t\t\t");
                buffer.append("<listOfReactants>\n");

                // Get the reactants -
                Vector<VLCGPBPKSpeciesModel> reactant_species_model_vector = (Vector)local_reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.BIOCHEMISTRY_REACTION_REACTANT_VECTOR);
                Iterator<VLCGPBPKSpeciesModel> reactant_model_iterator = reactant_species_model_vector.iterator();
                while (reactant_model_iterator.hasNext()){

                    // Get the model -
                    VLCGPBPKSpeciesModel reactant_model = (VLCGPBPKSpeciesModel)reactant_model_iterator.next();
                    String species_symbol = (String)reactant_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);
                    String species_coefficient = (String)reactant_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_COEFFICIENT);

                    buffer.append("\t\t\t\t\t<speciesReference species=\"");
                    buffer.append(species_symbol);
                    buffer.append("\" stoichiometry=\"");
                    buffer.append(species_coefficient);
                    buffer.append("\"/>\n");
                }

                buffer.append("\t\t\t\t");
                buffer.append("</listOfReactants>\n");

                // Get the products -
                buffer.append("\t\t\t\t");
                buffer.append("<listOfProducts>\n");
                Vector<VLCGPBPKSpeciesModel> product_species_model_vector = (Vector)local_reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.BIOCHEMISTRY_REACTION_PRODUCT_VECTOR);
                Iterator<VLCGPBPKSpeciesModel> product_model_iterator = product_species_model_vector.iterator();
                while (product_model_iterator.hasNext()){

                    // Get the model -
                    VLCGPBPKSpeciesModel product_model = (VLCGPBPKSpeciesModel)product_model_iterator.next();
                    String species_symbol = (String)product_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);
                    String species_coefficient = (String)product_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_COEFFICIENT);

                    buffer.append("\t\t\t\t\t<speciesReference species=\"");
                    buffer.append(species_symbol);
                    buffer.append("\" stoichiometry=\"");
                    buffer.append(species_coefficient);
                    buffer.append("\"/>\n");
                }

                buffer.append("\t\t\t\t");
                buffer.append("</listOfProducts>\n");
                buffer.append("\t\t\t");
                buffer.append("</reaction>\n");

                // update the index -
                reaction_index++;
            }

            buffer.append("\t\t");
            buffer.append("</location>\n");
        }

        // return -
        return buffer.toString();
    }


    private Vector<VLCGPBPKBiochemistryReactionModel> _findReactionsInCompartmentWithSymbol(String compartment_symbol, Vector<VLCGPBPKBiochemistryReactionModel> reaction_vector) throws Exception {

        // Method attributes -
        Vector<VLCGPBPKBiochemistryReactionModel> sorted_reaction_vector = new Vector<VLCGPBPKBiochemistryReactionModel>();

        // grab the reactions that are in this compartment ...
        int number_of_reactions = reaction_vector.size();
        for (int reaction_index = 0;reaction_index<number_of_reactions;reaction_index++){

            // Get reaction, check the compartment -
            VLCGPBPKBiochemistryReactionModel model = reaction_vector.get(reaction_index);
            String model_compartment = (String)model.getModelComponent(VLCGPBPKBiochemistryReactionModel.REACTION_COMPARTMENT_SYMBOL);

            if (model_compartment.equalsIgnoreCase(compartment_symbol) == true){
                sorted_reaction_vector.addElement(model);
            }
        }

        // return the sorted vector -
        return sorted_reaction_vector;
    }

    private String _generateListOfBiochemicalSpecies() throws Exception {

        // Method variables -
        StringBuilder buffer = new StringBuilder();
        Vector<String> tmp_species_symbol_vector = new Vector<String>();

        // Get the translation reactions -
        String class_name_key = _package_name_parser_delegate + ".VLCGPBPKCompartmentBiochemistryParserDelegate";
        Vector<VLCGPBPKModelComponent> control_vector = _model_component_table.get(Class.forName(class_name_key));
        Iterator<VLCGPBPKModelComponent> control_iterator = control_vector.iterator();
        while (control_iterator.hasNext()) {

            // get the connection model -
            VLCGPBPKBiochemistryReactionModel reaction_model = (VLCGPBPKBiochemistryReactionModel) control_iterator.next();
            reaction_model.doExecute();

            // get the reactants and products -
            Iterator<VLCGPBPKSpeciesModel> reactant_iterator = ((Vector)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.BIOCHEMISTRY_REACTION_REACTANT_VECTOR)).iterator();
            while (reactant_iterator.hasNext()){

                VLCGPBPKSpeciesModel model = reactant_iterator.next();

                // add this to the species collection?
                _addSpeciesModelToSpeciesModelList(model);
            }

            Iterator<VLCGPBPKSpeciesModel> product_iterator = ((Vector)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.BIOCHEMISTRY_REACTION_PRODUCT_VECTOR)).iterator();
            while (product_iterator.hasNext()) {

                VLCGPBPKSpeciesModel model = product_iterator.next();

                // add this to the species collection?
                _addSpeciesModelToSpeciesModelList(model);
            }

            // grab the enzyme if we have it -
            if (reaction_model.containsKey(VLCGPBPKBiochemistryReactionModel.BIOCHEMISTRY_REACTION_ENZYME_MODEL)){

                // ok, we have a model - get it and add to collection -
                VLCGPBPKSpeciesModel enzyme_model = (VLCGPBPKSpeciesModel)reaction_model.getModelComponent(VLCGPBPKBiochemistryReactionModel.BIOCHEMISTRY_REACTION_ENZYME_MODEL);

                System.out.println("Enzyme - "+enzyme_model);

                // add -
                _addSpeciesModelToSpeciesModelList(enzyme_model);
            }
        }

        // ok, we have a unqiue list of species models -
        Iterator<VLCGPBPKSpeciesModel> species_list = _species_model_vector.iterator();
        while (species_list.hasNext()){

            VLCGPBPKSpeciesModel model = species_list.next();
            String symbol = (String)model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);

            System.out.println(symbol);

            if (symbol.equalsIgnoreCase("[]") == false){

                buffer.append("\t\t");
                buffer.append("<species symbol=\"");
                buffer.append(symbol);
                buffer.append("\" initial_condition=\"0.0\"/>\n");
            }
        }

        // return -
        return buffer.toString();
    }


    private String _generateListOfCompartmentConnections() throws Exception {

        // Method variables -
        StringBuilder buffer = new StringBuilder();

        // Get the translation reactions -
        String class_name_key = _package_name_parser_delegate + ".VLCGPBPKCompartmentConnectionParserDelegate";
        Vector<VLCGPBPKModelComponent> control_vector = _model_component_table.get(Class.forName(class_name_key));
        Iterator<VLCGPBPKModelComponent> control_iterator = control_vector.iterator();
        int connection_index = 1;
        while (control_iterator.hasNext()) {

            // get the connection model -
            VLCGPBPKCompartmentConnectionModel connection_model = (VLCGPBPKCompartmentConnectionModel) control_iterator.next();

            // Get the source and target compartments -
            String source_compartment_symbol = (String) connection_model.getModelComponent(VLCGPBPKCompartmentConnectionModel.COMPARTMENT_CONNECTION_SOURCE);
            String target_compartment_symbol = (String) connection_model.getModelComponent(VLCGPBPKCompartmentConnectionModel.COMPARTMENT_CONNECTION_TARGET);
            String connection_name = (String)connection_model.getModelComponent(VLCGPBPKCompartmentConnectionModel.COMPARTMENT_CONNECTION_NAME);

            // need to check .. do we have a reverse connection?
            String reverse_flag = (String)connection_model.getModelComponent(VLCGPBPKCompartmentConnectionModel.COMPARTMENT_CONNECTION_REVERSE);

            // write xml lines -
            buffer.append("\t\t<connection index=\"");
            buffer.append(connection_index++);
            buffer.append("\" name=\"");
            buffer.append(connection_name);
            buffer.append("\" parameter_value=\"0.0\" start_symbol=\"");
            buffer.append(source_compartment_symbol);
            buffer.append("\" end_symbol=\"");
            buffer.append(target_compartment_symbol);
            buffer.append("\" />\n");

            if (reverse_flag.equalsIgnoreCase("0") == false){

                //System.out.print("Reverse - "+reverse_flag+" connection_name="+connection_name+"\n");

                // we have a reversible connection -
                buffer.append("\t\t<connection index=\"");
                buffer.append(connection_index++);
                buffer.append("\" name=\"");
                buffer.append(connection_name+"_reverse");
                buffer.append("\" parameter_value=\"0.0\" start_symbol=\"");
                buffer.append(target_compartment_symbol);
                buffer.append("\" end_symbol=\"");
                buffer.append(source_compartment_symbol);
                buffer.append("\" />\n");
            }
        }

        // return the buffer -
        return buffer.toString();
    }

    private String _generateListOfCompartments() throws Exception {


        // Method variables -
        StringBuilder buffer = new StringBuilder();

        // Get the translation reactions -
        String class_name_key = _package_name_parser_delegate + ".VLCGPBPKCompartmentConnectionParserDelegate";
        Vector<VLCGPBPKModelComponent> control_vector = _model_component_table.get(Class.forName(class_name_key));
        Iterator<VLCGPBPKModelComponent> control_iterator = control_vector.iterator();
        while (control_iterator.hasNext()) {

            // get the connection model -
            VLCGPBPKCompartmentConnectionModel connection_model = (VLCGPBPKCompartmentConnectionModel)control_iterator.next();

            // Get the source and target compartments -
            String source_compartment_symbol = (String)connection_model.getModelComponent(VLCGPBPKCompartmentConnectionModel.COMPARTMENT_CONNECTION_SOURCE);
            String target_compartment_symbol = (String)connection_model.getModelComponent(VLCGPBPKCompartmentConnectionModel.COMPARTMENT_CONNECTION_TARGET);

            // Create compartment models -
            VLCGPBPKCompartmentModel source_compartment_model = new VLCGPBPKCompartmentModel();
            source_compartment_model.setModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL,source_compartment_symbol);

            VLCGPBPKCompartmentModel target_compartment_model = new VLCGPBPKCompartmentModel();
            target_compartment_model.setModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL,target_compartment_symbol);

            // put the compartment models into the compartment vector -
            _addCompartmentModelToCompartmentModelList(source_compartment_model);
            _addCompartmentModelToCompartmentModelList(target_compartment_model);
        }

        Iterator compartment_iterator = _compartment_model_vector.iterator();
        int compartment_index = 1;
        while (compartment_iterator.hasNext()) {

            // get the current symbol -
            VLCGPBPKCompartmentModel compartment_current = (VLCGPBPKCompartmentModel) compartment_iterator.next();
            String current_symbol = (String) compartment_current.getModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL);

            // ok write the xml records -
            buffer.append("\t\t<compartment index=\"");
            buffer.append(compartment_index++);
            buffer.append("\" symbol=\"");
            buffer.append(current_symbol);
            buffer.append("\" initial_volume=\"");
            buffer.append("1.0\"/>\n");
        }

        // return the buffer -
        return buffer.toString();
    }


    private void _addSpeciesModelToSpeciesModelList(VLCGPBPKSpeciesModel model) throws Exception {

        // Get the symbol of the species that we are looking at ...
        String species_symbol = (String)model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);
        Boolean contains_species = false;

        if (species_symbol.equalsIgnoreCase("[]")){
            return;
        }

        Iterator<VLCGPBPKSpeciesModel> species_model_iterator = _species_model_vector.iterator();
        while (species_model_iterator.hasNext()){

            // Get the model -
            VLCGPBPKSpeciesModel local_model = species_model_iterator.next();
            String local_species = (String)local_model.getModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL);

            if (local_species.equalsIgnoreCase(species_symbol)){

                // we have a macth ... stop -
                contains_species = true;
                break;
            }
        }

        if (contains_species == false){
            _species_model_vector.addElement(model);
        }
    }


    private void _addCompartmentModelToCompartmentModelList(VLCGPBPKCompartmentModel compartmentModel) throws Exception {

        // get the compartment symbol -
        String test_compartment_symbol = (String)compartmentModel.getModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL);
        Boolean contains_compartment = false;

        if (test_compartment_symbol.equalsIgnoreCase("[]")) {
            return;
        }

        Iterator compartment_iterator = _compartment_model_vector.iterator();
        while (compartment_iterator.hasNext()){

            // get the current symbol -
            VLCGPBPKCompartmentModel compartment_current = (VLCGPBPKCompartmentModel)compartment_iterator.next();
            String current_symbol = (String)compartment_current.getModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL);
            if (current_symbol.equalsIgnoreCase(test_compartment_symbol)){

                contains_compartment = true;
                break;
            }
        }

        if (contains_compartment == false){
            _compartment_model_vector.addElement(compartmentModel);
        }
    }

    private void _readPBPKFlatFile(String fileName) throws Exception {

        // method allocation -
        VLCGParserHandlerDelegate parser_delegate = null;

        // check -
        if (fileName == null){
            throw new Exception("ERROR: Missing or null requirements for parsing the PBPK flat file.");
        }

        BufferedReader inReader = new BufferedReader(new FileReader(fileName));
        inReader.mark(0);
        String dataRecord = null;
        Vector<VLCGPBPKModelComponent> model_components_vector = null;
        while ((dataRecord = inReader.readLine()) != null) {

            int whitespace = dataRecord.length();

            // Need to check to make sure I have do not have a comment
            if (!dataRecord.contains("//") && whitespace != 0) {

                // Does this record start with a #pragma?
                if (dataRecord.contains("#pragma") == true){

                    // ok, this is a handler directive -
                    String[] tmp = dataRecord.split(" ");
                    String handler_class_name = tmp[tmp.length - 1];

                    // Create fully quaified class name -
                    String fully_qualified_handler_name = _package_name_parser_delegate+"."+handler_class_name;

                    // Create the handler -
                    parser_delegate = (VLCGParserHandlerDelegate)Class.forName(fully_qualified_handler_name).newInstance();

                    // Create a new vector -
                    model_components_vector = new Vector();
                }
                else {

                    // this is a "regular" line in the file -
                    // Do we have a parser handler?
                    if (parser_delegate == null){
                        throw new Exception("ERROR: The parser delegate is null. Check your #pragma parser directives.");
                    }

                    // If we get here, we have a parser delegate ...
                    VLCGPBPKModelComponent modelComponent = (VLCGPBPKModelComponent)parser_delegate.parseLine(dataRecord);
                    modelComponent.doExecute();

                    // add this component to the vector -
                    model_components_vector.addElement(modelComponent);

                    // Add this vector to the hashtable -
                    _model_component_table.put(parser_delegate.getClass(),model_components_vector);
                }
            }
        }

        // close -
        inReader.close();
    }
}
