########## Brianna Pritchett  ##########
########## bpritche           ##########

# This program is a bit of a test run for question edit distance - it is meant
# to take two files, one with questions (which will be given to us) and another
# one with questions and answers (which Dan and Maxime's program will produce)

import os, sys
import re, math
import string

def saveanswers(answerstring):
    answers = []
    for line in answerstring.splitlines():
        #line = line.replace('\n', '')
        answers.append(line)
    return answers
    
"""
questions = saveanswers(pqstring)
assert(len(questions) == len(ppostags))
#pqfile.close()
alist = saveanswers(astring)
assert(len(alist) == len(ppostags))
#afile.close()
"""

'''
def min_edit_distance(q1, q2):
    #HERE WE GO
    #Let's say that q1 and q2 are lists of tuples like ('fly', NN), so that we can
    #readily access both the word and POS tags.
    #q1 will be our target - the question we are trying to find an answer to.
    #q2 will be the source - the question we are considering.
    n = len(q1)
    m = len(q2)

    distance = []
    #Initialize the 2D list to all 0's.
    for i in xrange(0, n + 1):
        distance.append([])
        for j in xrange(0, m + 1):
            distance[i].append(0)
    
    #Here is where I'll put all of the insertion and deletion and substitution
    #costs for the different parts of speech.  See edit-rules.txt.
    
    for i in xrange(1, n + 1):
    	#This will give the POS tag for the corresponding word in the question.
    	pos_tag = q1[i - 1][1]
    	distance[i][0] = distance[i - 1][0] + ins_cost[pos_tag]
    	#Not sure what the best way to do this is, but this should give the insertion
    	#cost for a word of this tag.  This will initialize the condition where we 
    	# simply insert each letter into the empty string.
    for j in xrange(1, m + 1):
    	pos_tag = q2[j - 1][1]
    	distance[0][j] = distance[0][j - 1] + del_cost[pos_tag]
    for i in xrange(1, n + 1):
    	for j in xrange(1, m + 1):
    		tpos_tag = q1[i - 1][1]
    		spos_tag = q2[j - 1][1]
    		inscost = distance[i - 1][j] + ins_cost[tpos_tag]
    		delcost = distance[i][j - 1] + del_cost[spos_tag]
    		subcost = distance[i - 1][j - 1] + sub_cost[tpos_tag][spos_tag]
    		#other things to consider - transcost, changing the cost for subbing
    		# words that are synonyms vs. antonyms vs. not related to each other. 
    		#Also, for some it would be worth deciding whether hypernyms and hyponyms.
    return distance[n][m]
'''

def filter(index, qpostags, qdependencies, qsstags, ppostags, pdependencies, psstags):
    #index will represent the index of qpostags (or qdependencies or qsstags) that I'm
    # considering.
    # We only need that one argument because ppostags, pdependencies, and psstags are
    # global variables, so we can iterate over them without having to pass them in as 
    # arguments.
    
    #These will represent the list of pos tags, dependencies, and super sense tags 
    # (respectively) for the question I'm considering
    qpos = qpostags[index]
    qdep = qdependencies[index]
    qsst = qsstags[index]
    
    #This will be a list of the proper nouns, if any, in the question.  We
    # will do a regular expression to look for these in the questionlist
    # because if two questions aren't both about the same proper noun, it
    # would be difficult for them to be similar, since proper nouns don't
    # have synonyms.
    pnouns = []
    for i in xrange(0, len(qpos)):
        tagged = qpos[i]
        #The proper nouns are represented by the POS tags NNPS or NNP.
        if(re.match(r'(.*NNP)|(.*NNPS)', tagged) != None):
            pnouns.append(tagged)
    
    #pquestions will be a list of tuples.  The first will be the the index of the question, 
    # on.  I'm using indices because I have multiple lists, and the thing in common
    # between them is the index.  Thus, ppostags[i] will represent the question for alist[i]
    # (hopefully).  The second will the score I'm assigning it, based on the similarities 
    # between the questions.
    pquestionsi = []
    pquestionssim = []
            
    #Iterate over all possible questions
    for i in xrange(0, len(ppostags)):
        simscore = 0
        ppos = ppostags[i]
        pdep = pdependencies[i]
        psst = psstags[i]
        
        #I don't think this nested for loop is too big of a deal
        # because pnouns probably won't have more than a couple 
        # elements
        for tagpair in set(pnouns):
            if(tagpair in ppos):
                #This is very important!
                simscore += 20
        
        ssnum = 0
        for sstag in qsst:
            if(sstag != '0' and sstag in psst):
                ssnum += 1.0 
        ssprop = ssnum/float(len(qsst))
        #The reason I square it is because I want an exponential scale.  I don't want something
        # that's only 40% similar to get 4 points.  If I square it, it will get 1 point.
        # Something that's got 80% of the same tags will get 6 points, 90% will get 8.
        simscore += int((ssprop**2) * 10)
        
        for dep in qdep:
            depname = dep[0]
            if(dep in pdep):
                simscore += 3
                if(depname == 'amod' or depname == 'nn' or depname == 'nsubj'):
                    simscore += 10
                elif(depname == 'attr' or depname == 'root'):
                    simscore += 5
        
        threshold = 5
        if(simscore >= threshold):
            pquestionsi.append(i)
            pquestionssim.append(simscore)
        
    #if(len(pquestions) == 0): pquestions = questionlist
    #print pquestions
    return (pquestionsi, pquestionssim)

#outputfile = open(outputfilename, 'w+')

def processquestions(qtuple, ptuple, astring):

    (psquestions, ppostags, pdependencies, psstags) = ptuple
    assert(len(psquestions) == len(ppostags))
    assert(len(ppostags) == len(pdependencies))
    assert(len(pdependencies) == len(psstags))
    #pfile.close()

    (questions, qpostags, qdependencies, qsstags) = qtuple
    assert(len(questions) == len(qpostags))
    assert(len(qpostags) == len(qdependencies))
    assert(len(qdependencies) == len(qsstags))
    
    alist = saveanswers(astring)
    assert(len(alist) == len(ppostags))

    correct = 0
    total = 0
    for i in xrange(0, len(qpostags)):
        (pquestionsi, pquestionssim) = filter(i, qpostags, qdependencies, qsstags, ppostags, pdependencies, psstags)
        #print pquestions
        if(len(pquestionsi) == 1): 
            closest_index = 0
            qi = pquestionsi[0]
            answer = alist[qi]
            assocq = psquestions[qi]
        elif(len(pquestionsi) == 0): 
            print "This is a problem"
            #outputfile.write("This is a problem\n")
            answer = alist[0]
        #min_distance = 1000
        else:
            high_sim = 0
            closest_index = 0
            #cq_index = 0
            for j in xrange(0, len(pquestionsi)):
                qindex = pquestionsi[j]
                sim = pquestionssim[j]
                #edit_distance = min_edit_distance(line, pquestion)
                '''
                if(edit_distance == 0):
                    closest_question = pquestion
                    min_distance = 0
                    break
                if(edit_distance < min_distance):
                    min_distance = edit_distance
                    closest_question = pquestion
                    cq_index = i
                '''
                if(sim > high_sim):
                    closest_index = qindex
                    high_sim = sim
                answer = alist[closest_index]
                assocq = psquestions[closest_index]
            #outputfile.write(answer + '\n')
        print answer
        total += 1