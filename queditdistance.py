########## Brianna Pritchett  ##########
########## bpritche           ##########

# This program is a bit of a test run for question edit distance - it is meant
# to take two files, one with questions (which will be given to us) and another
# one with questions and answers (which Dan and Maxime's program will produce)

import os, sys
import re, math
import string

def filter(index, qpostags, qdependencies, qsstags, ppostags, pdependencies, psstags):
    #index will represent the index of qpostags (or qdependencies or qsstags) that I'm
    # considering.
    # We only need that one argument because ppostags, pdependencies, and psstags are
    # global variables, so we can iterate over them without having to pass them in as 
    # arguments.
    
    qpos = qpostags[index]
    qdep = qdependencies[index]
    qsst = qsstags[index]
    
    pquestionsi = []
    pquestionssim = []
    backupi = []
    backupsim = []
            
    #Iterate over all possible questions
    for i in xrange(0, len(ppostags)):
        simscore = 0
        ppos = ppostags[i]
        pdep = pdependencies[i]
        psst = psstags[i]
        
        if(qpos == ppos):
            pquestionsi = [i]
            pquestionssim = [simscore]
            break
            
        lendiff = abs(len(qpos) - len(ppos))
        if(lendiff < 5):
            backupi.append(i)
            backupsim.append(5 - lendiff)
            
        # Penalize sentences that have wildly different lengths
        simscore -= (min(0, abs(lendiff)) * min(0, abs(lendiff)))
        
        ssnum = 0
        qacttags = 0
        posnum = 0.0
        
        #Important dependencies
        impdep = ['amod', 'nn', 'nsubj', 'conj', 'attr', 'root', 'appos', 'dobj', 'neg', 'num',
                   'pobj', 'poss', 'preconj', 'predet', 'quantmod', 'tmod']
        for j in xrange(0, len(qsst)):
            postag = qpos[j]
            if(re.match(r'(.*NNP)|(.*NNPS)', postag) != None and postag in ppos):
                simscore += 30
            #Add in an element of the 'bag of words' similarity measure
            if(postag in ppos):
                posnum += 1.0
        
            sstag = qsst[j]
            if(sstag != '0'):
                qacttags += 1.0
                if(sstag in psst):
                    ssnum += 1.0
        posprop = posnum/float(len(qpos))
        ssprop = ssnum/qacttags
        #The reason I square it is because I want an exponential scale.  I don't want something
        # that's only 40% similar to get 4 points.  If I square it, it will get 1 point.
        # Something that's got 80% of the same tags will get 6 points, 90% will get 8.
        simscore += (int((ssprop**2) * 10) * 4)
        simscore += int((posprop**2) * 10) 
        
        for j in xrange(0, len(qdep)):            
            dep = qdep[j] 
            depname = dep[0]
            if(dep in pdep):
                simscore += 3
                if(depname in impdep):
                    simscore += 7
        
        threshold = 1
        if(simscore >= threshold):
            pquestionsi.append(i)
            pquestionssim.append(simscore)
        
    if(len(pquestionsi) == 0):
        if(len(backupsim) != 0): 
            pquestionsi = backupi
            pquestionsim = backupsim
        else:
            pquestionsi = [0]
            pquestionssim = [0]
    return (pquestionsi, pquestionssim)

def processquestions(qdict, pdict, alist):
    psquestions = pdict['questions']
    ppostags = pdict['postags']
    pdependencies = pdict['deptags']
    psstags = pdict['sstags']
    
    assert(len(psquestions) == len(ppostags))
    assert(len(ppostags) == len(pdependencies))
    assert(len(pdependencies) == len(psstags))

    questions = qdict['questions']
    qpostags = qdict['postags']
    qdependencies = qdict['deptags']
    qsstags = qdict['sstags']
    
    assert(len(questions) == len(qpostags))
    assert(len(qpostags) == len(qdependencies))
    assert(len(qdependencies) == len(qsstags))
    
    assert(len(alist) == len(ppostags))

    for i in xrange(0, len(qpostags)):
        (pquestionsi, pquestionssim) = filter(i, qpostags, qdependencies, qsstags, ppostags, pdependencies, psstags)
        #print pquestions
        if(len(pquestionsi) == 1): 
            closest_index = 0
            qi = pquestionsi[0]
            answer = alist[qi]
            assocq = psquestions[qi]
        elif(len(pquestionsi) == 0): 
            # print "This is a problem"
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
        print "Question is \"%s\" and answer is \"%s\".  Question associated with answer is \"%s\"\n" % (questions[i], answer, assocq)