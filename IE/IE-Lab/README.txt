Information Extraction - Using CRF++ Tool
_______________________________________________

FILES
-----

conll2002	directory containing data sets for training and testing

CRF++	directory containing the CRF++ tool and documentation

model	directory containing a POS-tagger model for Spanish 

eval.pl	script to evaluate the performance of CRF++ 

template-example	CRF++ template example

README.txt	this help file

______________________________________________
Assigment 4: Named Entity Recognition (NER) using CRF++
______________________________________________

1. Install CRF++ following the instructions found in the CRF++/doc folder

2. Build the feature templates. For an example see the template-example file.

3. Train NER using the training set (esp.train) in the conll2002/ folder

4. Test the classifier built in (3) using the test set (esp.testa) in the conll2002/ folder

5. Evaluate the performance of the classifier using the conll-eval.pl 

6. Repeat steps 2,3,4 and 5 until the performance is above 70% for each entity class.


7. Test the classifier built in (3) using the test set (esp.testb) in the conll2002/ folder

8. Write a Java NER application that used your classifier. Prepare your application to be tested by a new test set (provide in the grading lab). Note that before using your classifier to label the test set, you will need to apply a tokenizer and a POS-tagger (you can use the NLP code from the WWWLab and the Spanish POS-tagger model in the \textit{Information Extraction - Lab & Assignment resources IE-Lab:model:postag} posted to Wattle).

9. Your NER application must include a NE extractor 
that display the recognized entities in the one NE per line, organized by named entities categories, and display the frequency of each entity found (see the example/NE-ExtractorFormat.txt file) 

10. Use your NER application to find out the NE identified by your classifier when using the test set provided in the grading lab. 

















