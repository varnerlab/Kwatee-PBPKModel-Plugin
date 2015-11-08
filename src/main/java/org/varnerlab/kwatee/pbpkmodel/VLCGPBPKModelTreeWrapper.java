package org.varnerlab.kwatee.pbpkmodel;

import org.varnerlab.kwatee.pbpkmodel.model.VLCGPBPKBiochemistryControlModel;
import org.varnerlab.kwatee.pbpkmodel.model.VLCGPBPKCompartmentConnectionModel;
import org.varnerlab.kwatee.pbpkmodel.model.VLCGPBPKCompartmentModel;
import org.varnerlab.kwatee.pbpkmodel.model.VLCGPBPKSpeciesModel;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.NodeSetData;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

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
public class VLCGPBPKModelTreeWrapper {

    // instance variables -
    private Document _model_tree = null;
    private XPathFactory _xpath_factory = XPathFactory.newInstance();
    private XPath _xpath = _xpath_factory.newXPath();

    // cache some stuff so we don't do it twice -
    private ArrayList<VLCGPBPKSpeciesModel> _cached_species_list = new ArrayList<VLCGPBPKSpeciesModel>();
    private ArrayList<VLCGPBPKCompartmentModel> _cached_compartment_list = new ArrayList<VLCGPBPKCompartmentModel>();
    private ArrayList<VLCGPBPKCompartmentConnectionModel> _cached_compartment_connection_list = new ArrayList<VLCGPBPKCompartmentConnectionModel>();

    public VLCGPBPKModelTreeWrapper(Document document) {

        // grab the document -
        _model_tree = document;

        // init me -
        _init();
    }

    private void _init(){
    }


    public int calculateTheTotalNumberOfControlTerms() throws Exception {

        // method variables -
        int number_of_control_terms = 0;

        String xpath_string = ".//control/@name";
        NodeList nodeList = _lookupPropertyCollectionFromTreeUsingXPath(xpath_string);
        number_of_control_terms = nodeList.getLength();

        // return -
        return number_of_control_terms;
    }

    public int calculateTheTotalNumberOfReactionTerms() throws Exception {

        // method variables -
        int number_of_control_terms = 0;

        String xpath_string = ".//reaction/@name";
        NodeList nodeList = _lookupPropertyCollectionFromTreeUsingXPath(xpath_string);
        number_of_control_terms = nodeList.getLength();

        // return -
        return number_of_control_terms;
    }

    public String getStoichiometricCoefficientsForSpeciesInCompartment(String species_symbol,String compartment_symbol) throws Exception {

        // Method variables -
        StringBuilder row_buffer = new StringBuilder();

        // how many *total* reactions do we have?
        int number_of_reactions = this.calculateTheTotalNumberOfReactionTerms();

        // Build a tmp array of zeros -
        String[] tmp_array = new String[number_of_reactions];
        for (int col_index = 0;col_index<number_of_reactions;col_index++){
            tmp_array[col_index] = "0.0";
        }

        // ok, we need to get the stoichiometric coefficient for reactions having species_symbol in compartment as a reactant -
        String reaction_name_xpath = ".//listOfReactions/location[@compartment=\""+compartment_symbol+"\"]/reaction/listOfReactants/speciesReference[@species=\""+species_symbol+"\"]/../../@name";
        NodeList reaction_name_list = _lookupPropertyCollectionFromTreeUsingXPath(reaction_name_xpath);
        int number_of_local_reactions = reaction_name_list.getLength();
        for (int local_reaction_index = 0;local_reaction_index<number_of_local_reactions;local_reaction_index++){

            // Get name value -
            String local_reaction_name = reaction_name_list.item(local_reaction_index).getNodeValue();

            // Get the reaction index -
            String xpath_reaction_index = ".//listOfReactions/location[@compartment=\""+compartment_symbol+"\"]/reaction[@name=\""+local_reaction_name+"\"]/@index";
            String reaction_index = _lookupPropertyValueFromTreeUsingXPath(xpath_reaction_index);

            // Get stoichiometric coefficient -
            String xpath_stochiometric_coefficient = ".//listOfReactions/location[@compartment=\""+compartment_symbol+"\"]/reaction[@name=\""+local_reaction_name+"\"]/listOfReactants/speciesReference[@species=\""+species_symbol+"\"]/@stoichiometry";
            String stochiometric_coefficient = _lookupPropertyValueFromTreeUsingXPath(xpath_stochiometric_coefficient);

            // update the tmp_array -
            tmp_array[Integer.parseInt(reaction_index) - 1] = "-"+stochiometric_coefficient;
        }

        // Process the products -
        String product_name_xpath = ".//listOfReactions/location[@compartment=\""+compartment_symbol+"\"]/reaction/listOfProducts/speciesReference[@species=\""+species_symbol+"\"]/../../@name";
        NodeList product_reaction_name_list = _lookupPropertyCollectionFromTreeUsingXPath(product_name_xpath);
        number_of_local_reactions = product_reaction_name_list.getLength();
        for (int local_reaction_index = 0;local_reaction_index<number_of_local_reactions;local_reaction_index++){

            // Get name value -
            String local_reaction_name = product_reaction_name_list.item(local_reaction_index).getNodeValue();

            // Get the reaction index -
            String xpath_reaction_index = ".//listOfReactions/location[@compartment=\""+compartment_symbol+"\"]/reaction[@name=\""+local_reaction_name+"\"]/@index";
            String reaction_index = _lookupPropertyValueFromTreeUsingXPath(xpath_reaction_index);

            // Get stoichiometric coefficient -
            String xpath_stochiometric_coefficient = ".//listOfReactions/location[@compartment=\""+compartment_symbol+"\"]/reaction[@name=\""+local_reaction_name+"\"]/listOfProducts/speciesReference[@species=\""+species_symbol+"\"]/@stoichiometry";
            String stochiometric_coefficient = _lookupPropertyValueFromTreeUsingXPath(xpath_stochiometric_coefficient);

            // update the tmp_array -
            tmp_array[Integer.parseInt(reaction_index) - 1] = stochiometric_coefficient;
        }


        // populate the row_buffer -
        int col_index = 0;
        for (String element_value : tmp_array){
            row_buffer.append(element_value);

            if (col_index<number_of_reactions - 1){
                row_buffer.append(" ");
            }

            col_index++;
        }

        // add a new-line -
        row_buffer.append("\n");

        // return stoichiometric matrix row
        return row_buffer.toString();
    }

    public int findIndexOfConnectionBetweenStartAndEndCompartments(String start_compartment,String end_compartment) throws Exception {

        // method variables -
        int connection_index = -1;

        String xpath_string = ".//connection[@start_symbol=\""+start_compartment+"\"][@end_symbol=\""+end_compartment+"\"]/@index";
        connection_index = Integer.parseInt(_lookupPropertyValueFromTreeUsingXPath(xpath_string));

        // return -
        return connection_index;
    }

    public int findIndexForReactionWithNameInCompartment(String reaction_name,String compartment_symbol) throws Exception {

        // Method variables -
        int reaction_index = -1;

        String xpath_string = ".//listOfReactions/location[@compartment=\""+compartment_symbol+"\"]/reaction[@name=\""+reaction_name+"\"]/@index";
        reaction_index = Integer.parseInt(_lookupPropertyValueFromTreeUsingXPath(xpath_string));

        // return -
        return reaction_index;
    }

    public String getFirstCompartmentNameFromPBPKModelTree() throws Exception {

        // get compartent list -
        NodeList compartment_node_list = _lookupPropertyCollectionFromTreeUsingXPath(".//compartment");

        // Get the first node -
        Node compartment_node = compartment_node_list.item(0);
        NamedNodeMap attributes = compartment_node.getAttributes();

        return attributes.getNamedItem("symbol").getNodeValue();
    }


    public Boolean isSpeciesWithSymbolSubjectToRuleWithName(String species_symbol,String rule_name) throws Exception {

        // method variables -
        Boolean species_is_subject_to_rule = false;

        // Get rule type for this species?
        String xpath_string = ".//speciesRule[@species=\""+species_symbol+"\"]/@type";
        String ast_rule_name = _lookupPropertyValueFromTreeUsingXPath(xpath_string);
        if (ast_rule_name != null && ast_rule_name.equalsIgnoreCase(rule_name)){
            species_is_subject_to_rule = true;
        }

        // return default -
        return species_is_subject_to_rule;
    }

    public Boolean isSpeciesWithSymbolSubjectToRuleWithNameInCompartmentWithSymbol(String species_symbol,String rule_name,String compartment_symbol) throws Exception {

        // method variables -
        Boolean species_is_subject_to_rule = false;

        // Get rule type for this species?
        String xpath_string = ".//speciesRule[@species=\""+species_symbol+"\"][@compartment=\""+compartment_symbol+"\"]/@type";
        String ast_rule_name = _lookupPropertyValueFromTreeUsingXPath(xpath_string);
        if (ast_rule_name != null && ast_rule_name.equalsIgnoreCase(rule_name)){
            species_is_subject_to_rule = true;
        }

        // return default -
        return species_is_subject_to_rule;
    }

    public Boolean isThisASourceReaction(String reaction_name,String compartment_symbol) throws Exception {

        // Method variables -
        Boolean is_source_reaction = false;

        // if this is a degradation reaction, then we have [] as a product
        String xpath_string = ".//listOfReactions/location[@compartment=\""+compartment_symbol+"\"]/reaction[@name=\""+reaction_name+"\"]/listOfReactants/speciesReference/@species";
        NodeList node_list = _lookupPropertyCollectionFromTreeUsingXPath(xpath_string);
        int number_of_reactions = node_list.getLength();
        for (int reaction_index = 0;reaction_index<number_of_reactions;reaction_index++){

            // Get value -
            String product_symbol = node_list.item(reaction_index).getNodeValue();
            if (product_symbol.equalsIgnoreCase("[]")){

                is_source_reaction = true;
                break;
            }
        }

        // return -
        return is_source_reaction;
    }

    public Boolean isThisADegradationReaction(String reaction_name,String compartment_symbol) throws Exception {

        // Method variables -
        Boolean is_degradation_reaction = false;

        // if this is a degradation reaction, then we have [] as a product
        String xpath_string = ".//listOfReactions/location[@compartment=\""+compartment_symbol+"\"]/reaction[@name=\""+reaction_name+"\"]/listOfProducts/speciesReference/@species";
        NodeList node_list = _lookupPropertyCollectionFromTreeUsingXPath(xpath_string);
        int number_of_reactions = node_list.getLength();
        for (int reaction_index = 0;reaction_index<number_of_reactions;reaction_index++){

            // Get value -
            String product_symbol = node_list.item(reaction_index).getNodeValue();
            if (product_symbol.equalsIgnoreCase("[]")){

                is_degradation_reaction = true;
                break;
            }
        }

        return is_degradation_reaction;
    }

    public String getRawReactionStringFromPBPKModelTreeForReactionWithNameAndCompartment(String compartment_symbol,String reaction_name) throws Exception {

        if (compartment_symbol == null || compartment_symbol.isEmpty()){
            throw new Exception("Oops! Missing or null compartment symbol = failed to find associated reaction data.");
        }

        if (reaction_name == null || reaction_name.isEmpty()){
            throw new Exception("Oops! Missing or null reaction name = failed to find associated reaction data.");
        }

        // Formulate xpath and execute the query -
        String xpath_string = ".//listOfReactions/location[@compartment=\""+compartment_symbol+"\"]/reaction[@name=\""+reaction_name+"\"]/@formatted_raw_string";
        return _lookupPropertyValueFromTreeUsingXPath(xpath_string);
    }

    public ArrayList<String> getReactionNamesFromPBPKModelTreeInCompartmentWithSymbol(String compartment_symbol) throws Exception {

        if (compartment_symbol == null || compartment_symbol.isEmpty()){
            throw new Exception("Oops! Missing or null compartment symbol = failed to find associated reactions.");
        }


        // Method variables -
        ArrayList<String> reaction_name_array = new ArrayList<String>();

        // Formulate xpath and execute the query -
        String xpath_string = ".//listOfReactions/location[@compartment=\""+compartment_symbol+"\"]/reaction/@name";
        NodeList nodeList = _lookupPropertyCollectionFromTreeUsingXPath(xpath_string);
        int number_of_reactions = nodeList.getLength();
        for (int reaction_index = 0;reaction_index<number_of_reactions;reaction_index++){

            // Get the name -
            String reaction_name = nodeList.item(reaction_index).getNodeValue();
            reaction_name_array.add(reaction_name);
        }

        // return -
        return reaction_name_array;
    }


    public ArrayList<VLCGPBPKBiochemistryControlModel> getBiochemistryControlModelFromPBPKModelTreeForCompartmentWithSymbol(String compartment_symbol) throws Exception {

        if (compartment_symbol == null || compartment_symbol.isEmpty()){
            throw new Exception("Oops! Missing or null compartment symbol = failed to find associated control data.");
        }

        // Method variables -
        ArrayList<VLCGPBPKBiochemistryControlModel> control_model_array = new ArrayList<VLCGPBPKBiochemistryControlModel>();

        // Formulate xpath -
        String xpath_string = ".//control[@compartment=\""+compartment_symbol+"\"]";
        NodeList node_list = _lookupPropertyCollectionFromTreeUsingXPath(xpath_string);
        int number_of_control_terms = node_list.getLength();
        for (int control_term_index = 0;control_term_index<number_of_control_terms;control_term_index++){

            // get the compartment node -
            Node control_node = node_list.item(control_term_index);

            // Get data from compartment node -
            NamedNodeMap control_node_attributes = control_node.getAttributes();
            String name = control_node_attributes.getNamedItem("name").getNodeValue();
            String actor = control_node_attributes.getNamedItem("actor").getNodeValue();
            String target = control_node_attributes.getNamedItem("target").getNodeValue();
            String type = control_node_attributes.getNamedItem("type").getNodeValue();
            String raw_record = "name: "+name+" actor:"+actor+" target:"+target+" type:"+type+" compartment:"+compartment_symbol;

            // Create model -
            VLCGPBPKBiochemistryControlModel control_model = new VLCGPBPKBiochemistryControlModel();
            control_model.setModelComponent(VLCGPBPKBiochemistryControlModel.BIOCHEMISTRY_CONTROL_NAME,name);
            control_model.setModelComponent(VLCGPBPKBiochemistryControlModel.BIOCHEMISTRY_CONTROL_ACTOR,actor);
            control_model.setModelComponent(VLCGPBPKBiochemistryControlModel.BIOCHEMISTRY_CONTROL_TARGET,target);
            control_model.setModelComponent(VLCGPBPKBiochemistryControlModel.BIOCHEMISTRY_CONTROL_COMPARTMENT,compartment_symbol);
            control_model.setModelComponent(VLCGPBPKBiochemistryControlModel.BIOCHEMISTRY_CONTROL_TYPE,type);
            control_model.setModelComponent(VLCGPBPKBiochemistryControlModel.FORMATTED_RAW_RECORD,raw_record);

            // add model to array and go around again ...
            control_model_array.add(control_model);
        }

        // return the list -
        return control_model_array;
    }

    public ArrayList<String> makeSpeciesCompartmentSymbolArrayForPBPKModelTree() throws Exception {

        // Method variables -
        ArrayList<String> symbol_vector = new ArrayList<String>();
        ArrayList<VLCGPBPKCompartmentModel> compartment_model_list = getCompartmentModelsFromPBPKModelTree();
        NodeList species_node_list = _lookupPropertyCollectionFromTreeUsingXPath(".//species/@symbol");
        int number_of_species = species_node_list.getLength();

        // Use the cached vectors -
        for (VLCGPBPKCompartmentModel compartmentModel : compartment_model_list){

            for (int species_index = 0;species_index<number_of_species;species_index++){

                // build the symbol -
                Node species_node = species_node_list.item(species_index);
                String compartment_symbol = (String)compartmentModel.getModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL);
                String species_symbol = species_node.getNodeValue();
                String compound_symbol = species_symbol+"::"+compartment_symbol;
                symbol_vector.add(compound_symbol);
            }
        }

        // return -
        return symbol_vector;
    }

    public ArrayList<VLCGPBPKSpeciesModel> getReactantsForBiochemicalReactionWithNameInCompartment(String reaction_name,String compartment_symbol) throws Exception {

        // Method variables -
        ArrayList<VLCGPBPKSpeciesModel> species_vector = new ArrayList<VLCGPBPKSpeciesModel>();

        // xpath -
        String xpath_string = ".//location[@compartment=\""+compartment_symbol+"\"]/reaction[@name=\""+reaction_name+"\"]/listOfReactants/speciesReference";
        NodeList node_list = _lookupPropertyCollectionFromTreeUsingXPath(xpath_string);

        // create the list of species models -
        int number_of_species = node_list.getLength();
        for (int species_index = 0;species_index<number_of_species;species_index++){

            // Get the data -
            NamedNodeMap namedNodeMap = node_list.item(species_index).getAttributes();
            Node species_node = namedNodeMap.getNamedItem("species");
            Node coefficient_node = namedNodeMap.getNamedItem("stoichiometry");

            // Create the model -
            VLCGPBPKSpeciesModel species_model = new VLCGPBPKSpeciesModel();
            species_model.setModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL,species_node.getNodeValue());
            species_model.setModelComponent(VLCGPBPKSpeciesModel.SPECIES_COEFFICIENT,coefficient_node.getNodeValue());

            // cache -
            species_vector.add(species_model);
        }

        // return -
        return species_vector;
    }

    public Boolean isThisASourceCompartmentConnection(String connection_name) throws Exception {

        // Method variables -
        Boolean is_this_a_source_connection = false;

        // Setup xpath -
        String xpath_string = ".//connection[@name=\""+connection_name+"\"]/edge/@start_symbol";
        NodeList node_list = _lookupPropertyCollectionFromTreeUsingXPath(xpath_string);
        int number_of_compartment_symbols = node_list.getLength();

        for (int compartment_index = 0;compartment_index<number_of_compartment_symbols;compartment_index++){

            // get the symbol -
            String compartment_symbol = node_list.item(compartment_index).getNodeValue();
            if (compartment_symbol.equalsIgnoreCase("[]")){
                is_this_a_source_connection = true;
                break;
            }
        }

        // return -
        return is_this_a_source_connection;
    }

    public ArrayList<String> getCompartmentConnectionNamesFromPBPKModelTree() throws Exception {

        // Method variables -
        ArrayList<String> local_connection_list = new ArrayList<String>();
        NodeList connection_node_list = _lookupPropertyCollectionFromTreeUsingXPath(".//connection/@name");
        int number_of_connections = connection_node_list.getLength();
        for (int connection_index = 0;connection_index<number_of_connections;connection_index++){

            // Get the connection node -
            Node connection_node = connection_node_list.item(connection_index);

            // Grab the name -
            String connection_name = connection_node.getNodeValue();
            local_connection_list.add(connection_name);
        }

        // return -
        return local_connection_list;
    }

    public ArrayList<String> getListOfCompartmentsDownstreamOfCompartmentWithSymbol(String start_compartment_symbol) throws Exception {

        // method variables -
        ArrayList<String> compartment_name_array = new ArrayList<String>();

        // find all comparment names that are connected -
        String xpath_string = ".//connection[@start_symbol=\""+start_compartment_symbol+"\"]/@end_symbol";
        NodeList compartment_node_list = _lookupPropertyCollectionFromTreeUsingXPath(xpath_string);
        int number_of_compartments = compartment_node_list.getLength();
        for (int compartment_index = 0;compartment_index<number_of_compartments;compartment_index++) {

            // get the compartment node -
            Node compartment_node = compartment_node_list.item(compartment_index);

            // Get connected name -
            String connected_compartment_symbol = compartment_node.getNodeValue();

            // add the connected name to the tmp name vector (we need this later)
            compartment_name_array.add(connected_compartment_symbol);
        }

        // return -
        return compartment_name_array;
    }

    public ArrayList<String> getListOfCompartmentsUpstreamOfCompartmentWithSymbol(String start_compartment_symbol) throws Exception {

        // method variables -
        ArrayList<String> compartment_name_array = new ArrayList<String>();

        // find all comparment names that are connected -
        String xpath_string = ".//connection[@end_symbol=\""+start_compartment_symbol+"\"]/@start_symbol";
        NodeList compartment_node_list = _lookupPropertyCollectionFromTreeUsingXPath(xpath_string);
        int number_of_compartments = compartment_node_list.getLength();
        for (int compartment_index = 0;compartment_index<number_of_compartments;compartment_index++) {

            // get the compartment node -
            Node compartment_node = compartment_node_list.item(compartment_index);

            // Get connected name -
            String connected_compartment_symbol = compartment_node.getNodeValue();

            // add the connected name to the tmp name vector (we need this later)
            compartment_name_array.add(connected_compartment_symbol);
        }

        // return -
        return compartment_name_array;
    }

    public ArrayList<VLCGPBPKCompartmentModel> getCompartmentModelsFromPBPKModelTree() throws Exception {

        // do we have a compartment list already? => yes, return the cached list
        if (_cached_compartment_list.size()>0){
            return _cached_compartment_list;
        }

        // method variables -
        ArrayList<VLCGPBPKCompartmentModel> compartment_model_array = new ArrayList<VLCGPBPKCompartmentModel>();

        // lookup compartments -
        NodeList compartment_node_list = _lookupPropertyCollectionFromTreeUsingXPath(".//compartment");
        int number_of_compartments = compartment_node_list.getLength();
        for (int compartment_index = 0;compartment_index<number_of_compartments;compartment_index++) {

            // get the compartment node -
            Node compartment_node = compartment_node_list.item(compartment_index);

            // Get data from compartment node -
            NamedNodeMap compartment_node_attributes = compartment_node.getAttributes();
            String compartment_name = compartment_node_attributes.getNamedItem("symbol").getNodeValue();
            String compartment_volume = compartment_node_attributes.getNamedItem("initial_volume").getNodeValue();

            // Create compartment model -
            VLCGPBPKCompartmentModel model = new VLCGPBPKCompartmentModel();
            model.setModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_SYMBOL,compartment_name);
            model.setModelComponent(VLCGPBPKCompartmentModel.COMPARTMENT_VOLUME,compartment_volume);

            // cache -
            compartment_model_array.add(model);
        }

        // cache -
        _cached_compartment_list.clear();
        _cached_compartment_list.addAll(compartment_model_array);

        // return -
        return compartment_model_array;
    }


    public ArrayList<VLCGPBPKSpeciesModel> getSpeciesModelsFromPBPKModelTree() throws Exception {

        // do we have a species list already? => yes, return the cached list
        if (_cached_species_list.size()>0){
            return _cached_species_list;
        }


        // method variables -
        ArrayList<VLCGPBPKSpeciesModel> species_model_array = new ArrayList<VLCGPBPKSpeciesModel>();


        // Get the species nodeset from the AST -
        NodeList species_node_list = _lookupPropertyCollectionFromTreeUsingXPath(".//species");
        NodeList compartment_node_list = _lookupPropertyCollectionFromTreeUsingXPath(".//compartment");

        // iterate through the compartment list -
        int number_of_compartments = compartment_node_list.getLength();
        int number_of_species = species_node_list.getLength();
        for (int compartment_index = 0;compartment_index<number_of_compartments;compartment_index++){

            // get the compartment node -
            Node compartment_node = compartment_node_list.item(compartment_index);

            // Get data from compartment node -
            NamedNodeMap compartment_node_attributes = compartment_node.getAttributes();
            String compartment_name = compartment_node_attributes.getNamedItem("symbol").getNodeValue();
            String compartment_volume = compartment_node_attributes.getNamedItem("initial_volume").getNodeValue();

            // ok, iterate through the list of species -
            for (int species_index = 0;species_index<number_of_species;species_index++){

                // Get the species node -
                Node species_node = species_node_list.item(species_index);

                // Get the data from the species node -
                NamedNodeMap species_node_attributes = species_node.getAttributes();
                String species_symbol = species_node_attributes.getNamedItem("symbol").getNodeValue();
                String species_initial_condition = species_node_attributes.getNamedItem("initial_condition").getNodeValue();

                // build the species model -
                VLCGPBPKSpeciesModel species_model = new VLCGPBPKSpeciesModel();
                species_model.setModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL,species_symbol);
                species_model.setModelComponent(VLCGPBPKSpeciesModel.SPECIES_COMPARTMENT,compartment_name);
                species_model.setModelComponent(VLCGPBPKSpeciesModel.SPECIES_INITIAL_CONDITION,species_initial_condition);
                species_model.setModelComponent(VLCGPBPKSpeciesModel.SPECIES_SPECIES_TYPE,"biochemical");

                // cache -
                species_model_array.add(species_model);
            }
        }

        // ok, last thing - add the volumes for each compartment -
        for (int compartment_index = 0;compartment_index<number_of_compartments;compartment_index++) {

            // get the compartment node -
            Node compartment_node = compartment_node_list.item(compartment_index);

            // Get data from compartment node -
            NamedNodeMap compartment_node_attributes = compartment_node.getAttributes();
            String compartment_name = compartment_node_attributes.getNamedItem("symbol").getNodeValue();
            String compartment_volume = compartment_node_attributes.getNamedItem("initial_volume").getNodeValue();

            // create a species for the volume -
            VLCGPBPKSpeciesModel species_model = new VLCGPBPKSpeciesModel();
            species_model.setModelComponent(VLCGPBPKSpeciesModel.SPECIES_SYMBOL,"volume_"+compartment_name);
            species_model.setModelComponent(VLCGPBPKSpeciesModel.SPECIES_INITIAL_CONDITION,compartment_volume);
            species_model.setModelComponent(VLCGPBPKSpeciesModel.SPECIES_COMPARTMENT,compartment_name);
            species_model.setModelComponent(VLCGPBPKSpeciesModel.SPECIES_SPECIES_TYPE,"volume");

            // cache -
            species_model_array.add(species_model);
        }


        // add all of the species models to the cache -
        _cached_species_list.clear();
        _cached_species_list.addAll(species_model_array);

        // return -
        return species_model_array;
    }


    // private helper methods -
    private NodeList _lookupPropertyCollectionFromTreeUsingXPath(String xpath_string) throws Exception {

        if (xpath_string == null) {
            throw new Exception("Null xpath in property lookup call.");
        }

        // Exceute the xpath -
        NodeList node_list = null;
        try {

            node_list = (NodeList) _xpath.evaluate(xpath_string, _model_tree, XPathConstants.NODESET);

        }
        catch (Exception error) {
            error.printStackTrace();
            System.out.println("ERROR: Property lookup failed. The following XPath "+xpath_string+" resulted in an error - "+error.toString());
        }

        // return -
        return node_list;
    }

    /**
     * Return the string value obtained from executing the XPath query passed in as an argument
     * @param xpath_string hold the xpath statement to search the tree
     * @return String - get property from uxml tree by executing string in strXPath
     */
    private String _lookupPropertyValueFromTreeUsingXPath(String xpath_string) throws Exception {

        if (xpath_string == null)
        {
            throw new Exception("ERROR: Null xpath in property lookup call.");
        }

        // Method attributes -
        String property_string = null;

        try {
            Node propNode = (Node) _xpath.evaluate(xpath_string, _model_tree, XPathConstants.NODE);
            if (propNode != null){

                property_string = propNode.getNodeValue();
            }
        }
        catch (Exception error)
        {
            error.printStackTrace();
            System.out.println("ERROR: Property lookup failed. The following XPath "+xpath_string+" resulted in an error - "+error.toString());
        }

        return property_string;
    }

}
