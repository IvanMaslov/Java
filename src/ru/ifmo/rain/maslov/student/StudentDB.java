package ru.ifmo.rain.maslov.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.stream.*;

public class StudentDB implements StudentGroupQuery {

    public StudentDB() {
    }

    private Comparator<Student> nameComparator =
            Comparator.comparing(Student::getLastName)
                    .thenComparing(Student::getFirstName);

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return students.stream()
                .map(Student::getFirstName)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return students.stream()
                .map(Student::getLastName)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return students.stream()
                .map(Student::getGroup)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return students.stream()
                .map(x -> x.getFirstName() + " " + x.getLastName())
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students.stream()
                .map(Student::getFirstName)
                .collect(Collectors.toSet());
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream()
                .min(Comparator.comparing(Student::getId))
                .get()
                .getFirstName();
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return students.stream()
                .sorted(Comparator.comparing(Student::getId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return students.stream()
                .sorted(nameComparator.thenComparing(Student::getId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return students.stream()
                .filter(x -> name.equals(x.getFirstName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return students.stream()
                .filter(x -> name.equals(x.getLastName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return students.stream()
                .filter(x -> group.equals(x.getGroup()))
                .sorted(nameComparator.thenComparing(Student::getId))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return students.stream()
                .filter(x -> group.equals(x.getGroup()))
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        (a, b) -> a.compareTo(b) < 0 ? a : b
                ));
    }

    private List<Group> getGroupBy(Collection<Student> students, Comparator<Student> comp) {
        return students.stream()
                .sorted(comp)
                .collect(Collectors.groupingBy(
                        Student::getGroup,
                        Collectors.toList()))
                .entrySet()
                .stream()
                .map(x -> new Group(x.getKey(), x.getValue()))
                .collect(Collectors.toList());

    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupBy(students, nameComparator.thenComparing(Student::getId));
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupBy(students, Comparator.comparing(Student::getId));
    }

    private String getLargestGroupBy(Collection<Student> students, Comparator<Group> comp) {
        return getGroupsByName(students).stream().max(comp).get().getName();
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestGroupBy(students, Comparator.comparingInt(a -> a.getStudents().size()));
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupBy(students, Comparator.comparingLong(a ->
                a.getStudents()
                        .stream()
                        .map(Student::getFirstName)
                        .distinct()
                        .count()));
    }
}

// java -cp . -p . -m info.kgeorgiy.java.advanced.student StudentGroupQuery  ru.ifmo.rain.maslov.student.StudentDB hello
